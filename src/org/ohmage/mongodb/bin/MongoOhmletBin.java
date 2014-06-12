package org.ohmage.mongodb.bin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.mongojack.DBCursor;
import org.mongojack.DBQuery;
import org.mongojack.DBQuery.Query;
import org.mongojack.Id;
import org.mongojack.JacksonDBCollection;
import org.mongojack.ObjectId;
import org.mongojack.WriteResult;
import org.ohmage.bin.MultiValueResult;
import org.ohmage.bin.OhmletBin;
import org.ohmage.domain.OhmageDomainObject;
import org.ohmage.domain.Schema;
import org.ohmage.domain.exception.InconsistentDatabaseException;
import org.ohmage.domain.exception.InvalidArgumentException;
import org.ohmage.domain.ohmlet.Ohmlet;
import org.ohmage.domain.ohmlet.Ohmlet.Member;
import org.ohmage.domain.ohmlet.Ohmlet.SchemaReference;
import org.ohmage.mongodb.domain.MongoDbObject;
import org.ohmage.mongodb.domain.MongoOhmlet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mongodb.AggregationOutput;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import com.mongodb.QueryBuilder;

/**
 * <p>
 * The MongoDB implementation of the database-backed ohmlet repository.
 * </p>
 *
 * @author John Jenkins
 */
public class MongoOhmletBin extends OhmletBin {
	/**
	 * The name of the collection that contains all of the communities.
	 */
	public static final String COLLECTION_NAME = "ohmlet_bin";

    /**
     * <p>
     * An anonymous class to represent the members that will be returned.
     * </p>
     *
     * @author John Jenkins
     */
    private static class MemberInfo implements MongoDbObject {
        /**
         * The database ID for this object.
         */
        @ObjectId
        private final String dbId;

        /**
         * The list of members.
         */
        @JsonProperty(Ohmlet.JSON_KEY_MEMBERS)
        public final List<Member> members = new LinkedList<Member>();

        /**
         * Creates a new MemberInfo object.
         *
         * @param dbId
         *        The unique identifier generated by the database.
         *
         * @param members
         *        The list of members.
         */
        @JsonCreator
        public MemberInfo(
            @Id @ObjectId final String dbId,
            @JsonProperty(Ohmlet.JSON_KEY_MEMBERS)
                final List<Member> members) {

            this.dbId = dbId;
            this.members.addAll(members);
        }

        /*
         * (non-Javadoc)
         * @see org.ohmage.mongodb.domain.MongoDbObject#getDbId()
         */
        @Override
        public String getDbId() {
            return dbId;
        }
    }

	/**
	 * Get the connection to the ohmlet bin with the Jackson wrapper.
	 */
	private static final JacksonDBCollection<Ohmlet, Object> COLLECTION =
		JacksonDBCollection
			.wrap(
				MongoBinController
					.getInstance()
					.getDb()
					.getCollection(COLLECTION_NAME),
				Ohmlet.class,
				Object.class,
				MongoBinController.getObjectMapper());

	/**
	 * Get the connection to the ohmlet bin with the Jackson wrapper,
	 * specifically for {@link MongoOhmlet} objects.
	 */
	private static final JacksonDBCollection<MongoOhmlet, Object> MONGO_COLLECTION =
		JacksonDBCollection
			.wrap(
				MongoBinController
					.getInstance()
					.getDb()
					.getCollection(COLLECTION_NAME),
				MongoOhmlet.class,
				Object.class,
				MongoBinController.getObjectMapper());

	/**
	 * Default constructor.
	 */
	protected MongoOhmletBin() {
		// Ensure that there is an index on the ID.
		COLLECTION
			.ensureIndex(
				new BasicDBObject(Ohmlet.JSON_KEY_ID, 1),
				COLLECTION_NAME + "_" + Ohmlet.JSON_KEY_ID,
				false);
	}

	/*
	 * (non-Javadoc)
	 * @see org.ohmage.bin.OhmletBin#addOhmlet(org.ohmage.domain.Ohmlet)
	 */
	@Override
	public void addOhmlet(
		final Ohmlet ohmlet)
		throws IllegalArgumentException, IllegalStateException {

		// Validate the parameter.
		if(ohmlet == null) {
			throw new IllegalArgumentException("The ohmlet is null.");
		}

		// Save it.
		try {
			COLLECTION.insert(ohmlet);
		}
		catch(MongoException.DuplicateKey e) {
			throw
				new InvalidArgumentException(
					"A stream with the same ID-version pair already exists.");
		}
	}

    /*
     * (non-Javadoc)
     * @see org.ohmage.bin.OhmletBin#getOhmletIds(java.lang.String, java.lang.String)
     */
    @Override
    public MultiValueResult<String> getOhmletIds(
        final String userId,
        final String query,
        final long numToSkip,
        final long numToReturn) {
        ArrayList<String> results = new ArrayList<String>();
        MultiValueResult<Ohmlet> ohmlets = getOhmlets(userId, query, numToSkip, numToReturn);
        for(Ohmlet ohmlet : ohmlets) {
            results.add(ohmlet.getId());
        }
        return new MongoMultiValueResultList<String>(results, results.size());
    }

	/*
	 * (non-Javadoc)
	 * @see org.ohmage.bin.OhmletBin#getOhmlets(java.lang.String, java.lang.String)
	 */
	@Override
	public MultiValueResult<Ohmlet> getOhmlets(
		final String userId,
		final String query,
        final long numToSkip,
        final long numToReturn) {

		// Build the query
		QueryBuilder queryBuilder = QueryBuilder.start();

		// Be sure that they are visible.
		List<DBObject> visibilityModifiers = new LinkedList<DBObject>();
        // Either, the ohmlet's privacy state must be INVITE_ONLY+.
		visibilityModifiers
		    .add(
                QueryBuilder
                    .start()
                    .and(Ohmlet.JSON_KEY_PRIVACY_STATE)
                    .greaterThanEquals(
                        Ohmlet.PrivacyState.INVITE_ONLY.ordinal())
                    .get());
        // Or, the user must already be a member, if given.
		if(userId != null) {
		    visibilityModifiers
		        .add(
                    QueryBuilder
                        .start()
                        .and(Ohmlet.JSON_KEY_MEMBERS +
                            "." +
                            Ohmlet.Member.JSON_KEY_MEMBER_ID)
                        .is(userId)
                        .get());
		}
		// Build the query.
		queryBuilder
			.and(
				QueryBuilder
					.start()
					.or(visibilityModifiers.toArray(new DBObject[0]))
					.get());

		// If given, add the query for the name and description.
		if(query != null) {
			// Build the query pattern.
			Pattern queryPattern = Pattern.compile(".*" + query + ".*");

			// Add the query fields.
			queryBuilder
				.and(
					QueryBuilder
						.start()
						.or(
							// Add the query for the name.
							QueryBuilder
								.start()
								.and(Schema.JSON_KEY_NAME)
								.regex(queryPattern)
								.get(),
							// Add the query for the description.
							QueryBuilder
								.start()
								.and(Schema.JSON_KEY_VERSION)
								.regex(queryPattern)
								.get())
						.get());
		}

        BasicDBObjectBuilder fields = BasicDBObjectBuilder
            .start()
            .add(Schema.JSON_KEY_ID , 1 )
            .add(Schema.JSON_KEY_NAME , 1 )
            .add(Schema.JSON_KEY_DESCRIPTION, 1);


		// Get the list of results.
		@SuppressWarnings("unchecked")
		DBCursor<Ohmlet> results =
			COLLECTION.find(queryBuilder.get()).skip((int)numToSkip).limit((int)numToReturn);

        // Sort the results.
        results.sort(BasicDBObjectBuilder.start().add(Schema.JSON_KEY_ID,1).get());

        // Create a MultiValueResult.
        MultiValueResult<Ohmlet> result =
            new MongoMultiValueResultList<Ohmlet>(results.toArray(), results.size());

		return result;
	}

    /*
     * (non-Javadoc)
     * @see org.ohmage.bin.OhmletBin#getOhmletIdsWhereUserCanReadStreamData(java.lang.String, java.lang.String, long, boolean)
     */
    @Override
    public Set<String> getOhmletIdsWhereUserCanReadStreamData(
        final String userId,
        final String streamId,
        final long streamVersion,
        final boolean allowNull)
        throws IllegalArgumentException {

        // Validate the input.
        if(userId == null) {
            throw new IllegalArgumentException("The user ID is null.");
        }
        if(streamId == null) {
            throw new IllegalArgumentException("The stream ID is null.");
        }

        // Make sure the user is a member of the ohmlet.
        DBObject userIsMember =
            new BasicDBObject(
                "$match",
                new BasicDBObject(
                    Ohmlet.JSON_KEY_MEMBERS +
                        "." +
                        Ohmlet.Member.JSON_KEY_MEMBER_ID,
                    userId));

        // Make sure the ohmlet references the stream.
        DBObject ohmletReferencesStreamId =
            new BasicDBObject(
                "$match",
                new BasicDBObject(
                    Ohmlet.JSON_KEY_STREAMS +
                        "." +
                        SchemaReference.JSON_KEY_SCHEMA_ID,
                    streamId));

        // Make sure the ohmlet is the same as the given version.
        DBObject ohmletReferencesStreamVersion;
        DBObject isValue =
            new BasicDBObject(
                Ohmlet.JSON_KEY_STREAMS +
                    "." +
                    SchemaReference.JSON_KEY_VERSION,
                streamVersion);
        // If it allowed to be null, then we will have an OR here.
        if(allowNull) {
            DBObject isNull =
                new BasicDBObject(
                    Ohmlet.JSON_KEY_STREAMS +
                        "." +
                        SchemaReference.JSON_KEY_VERSION,
                    null);

            BasicDBList or = new BasicDBList();
            or.add(isValue);
            or.add(isNull);

            ohmletReferencesStreamVersion =
                new BasicDBObject("$match", new BasicDBObject("$or", or));
        }
        // Otherwise, it must be the given version.
        else {
            ohmletReferencesStreamVersion =
                new BasicDBObject("$match", isValue);
        }

        // Create the projection that retains only the ohmlet ID and the
        // computed value as to whether or not the user can read data.
        DBObject projectionFields = new BasicDBObject();
        projectionFields.put(Ohmlet.JSON_KEY_ID, 1);

        BasicDBList gteFields = new BasicDBList();
        gteFields
            .add("$" + Ohmlet.JSON_KEY_MEMBERS + "." + Member.JSON_KEY_ROLE);
        gteFields.add("$" + Ohmlet.JSON_KEY_VISIBILITY_ROLE);
        projectionFields.put("can_read", new BasicDBObject("$gte", gteFields));

        DBObject projection = new BasicDBObject("$project", projectionFields);

        // Make sure the user has the read permission for the given ohmlet.
        DBObject canRead =
            new BasicDBObject("$match", new BasicDBObject("can_read", true));

        // Perform the aggregation.
        AggregationOutput output =
            COLLECTION
                .getDbCollection()
                .aggregate(
                    userIsMember,
                    ohmletReferencesStreamId,
                    ohmletReferencesStreamVersion,
                    projection,
                    canRead);

        // Get the iterator for the results.
        Iterator<DBObject> iterator = output.results().iterator();

        // Compute the results.
        Set<String> results = new HashSet<String>();
        while(iterator.hasNext()) {
            // Get the next element.
            DBObject element = iterator.next();

            // Add the ohmlet's ID.
            results.add(element.get(Ohmlet.JSON_KEY_ID).toString());
        }

        // Return the results.
        return results;
    }

    /*
     * (non-Javadoc)
     * @see org.ohmage.bin.OhmletBin#getOhmletIdsWhereUserCanReadSurveyResponses(java.lang.String, java.lang.String, long, boolean)
     */
    @Override
    public Set<String> getOhmletIdsWhereUserCanReadSurveyResponses(
        final String userId,
        final String surveyId,
        final long surveyVersion,
        final boolean allowNull)
        throws IllegalArgumentException {

        // Validate the input.
        if(userId == null) {
            throw new IllegalArgumentException("The user ID is null.");
        }
        if(surveyId == null) {
            throw new IllegalArgumentException("The survey ID is null.");
        }

        // Make sure the user is a member of the ohmlet.
        DBObject userIsMember =
            new BasicDBObject(
                "$match",
                new BasicDBObject(
                    Ohmlet.JSON_KEY_MEMBERS +
                        "." +
                        Ohmlet.Member.JSON_KEY_MEMBER_ID,
                    userId));

        // Make sure the ohmlet references the survey.
        DBObject ohmletReferencesSurveyId =
            new BasicDBObject(
                "$match",
                new BasicDBObject(
                    Ohmlet.JSON_KEY_SURVEYS +
                        "." +
                        SchemaReference.JSON_KEY_SCHEMA_ID,
                    surveyId));

        // Make sure the ohmlet is the same as the given version.
        DBObject ohmletReferencesSurveyVersion;
        DBObject isValue =
            new BasicDBObject(
                Ohmlet.JSON_KEY_SURVEYS +
                    "." +
                    SchemaReference.JSON_KEY_VERSION,
                surveyVersion);
        // If it allowed to be null, then we will have an OR here.
        if(allowNull) {
            DBObject isNull =
                new BasicDBObject(
                    Ohmlet.JSON_KEY_SURVEYS +
                        "." +
                        SchemaReference.JSON_KEY_VERSION,
                    null);

            BasicDBList or = new BasicDBList();
            or.add(isValue);
            or.add(isNull);

            ohmletReferencesSurveyVersion =
                new BasicDBObject("$match", new BasicDBObject("$or", or));
        }
        // Otherwise, it must be the given version.
        else {
            ohmletReferencesSurveyVersion =
                new BasicDBObject("$match", isValue);
        }

        // Create the projection that retains only the ohmlet ID and the
        // computed value as to whether or not the user can read data.
        DBObject projectionFields = new BasicDBObject();
        projectionFields.put(Ohmlet.JSON_KEY_ID, 1);

        BasicDBList gteFields = new BasicDBList();
        gteFields
            .add("$" + Ohmlet.JSON_KEY_MEMBERS + "." + Member.JSON_KEY_ROLE);
        gteFields.add("$" + Ohmlet.JSON_KEY_VISIBILITY_ROLE);
        projectionFields.put("can_read", new BasicDBObject("$gte", gteFields));

        DBObject projection = new BasicDBObject("$project", projectionFields);

        // Make sure the user has the read permission for the given ohmlet.
        DBObject canRead =
            new BasicDBObject("$match", new BasicDBObject("can_read", true));

        // Perform the aggregation.
        AggregationOutput output =
            COLLECTION
                .getDbCollection()
                .aggregate(
                    userIsMember,
                    ohmletReferencesSurveyId,
                    ohmletReferencesSurveyVersion,
                    projection,
                    canRead);

        // Get the iterator for the results.
        Iterator<DBObject> iterator = output.results().iterator();

        // Compute the results.
        Set<String> results = new HashSet<String>();
        while(iterator.hasNext()) {
            // Get the next element.
            DBObject element = iterator.next();

            // Add the ohmlet's ID.
            results.add(element.get(Ohmlet.JSON_KEY_ID).toString());
        }

        // Return the results.
        return results;
    }

	/*
	 * (non-Javadoc)
	 * @see org.ohmage.bin.OhmletBin#getOhmlet(java.lang.String, java.lang.Long)
	 */
	@Override
	public Ohmlet getOhmlet(
		final String ohmletId)
		throws IllegalArgumentException {

		// Validate the input.
		if(ohmletId == null) {
			throw new IllegalArgumentException("The ohmlet ID is null.");
		}

		// Build the query
		QueryBuilder queryBuilder = QueryBuilder.start();

		// Add the ohmlet ID.
		queryBuilder.and(Ohmlet.JSON_KEY_ID).is(ohmletId);

		// Execute query.
		return MONGO_COLLECTION.findOne(queryBuilder.get());
	}

	/*
	 * (non-Javadoc)
	 * @see org.ohmage.bin.OhmletBin#getMembers(java.util.Collection)
	 */
    @Override
    public Set<String> getMemberIds(
        final Collection<String> ohmletIds)
        throws IllegalArgumentException {

        // Validate the input.
        if(ohmletIds == null) {
            throw new IllegalArgumentException("The ohmlet ID list is null.");
        }

        // Build the query
        QueryBuilder queryBuilder = QueryBuilder.start();

        // Make sure to only return the ohmlet's whose ID is in question.
        queryBuilder.and(Ohmlet.JSON_KEY_ID).in(ohmletIds);

        // Create the collection that echos this class.
        JacksonDBCollection<MemberInfo, Object> memberCollection =
            JacksonDBCollection
                .wrap(
                    MongoBinController
                        .getInstance()
                        .getDb()
                        .getCollection(COLLECTION_NAME),
                    MemberInfo.class,
                    Object.class,
                    MongoBinController.getObjectMapper());

        // Make the query.
        DBCursor<MemberInfo> members =
            memberCollection
                .find(
                    queryBuilder.get(),
                    new BasicDBObject(Ohmlet.JSON_KEY_MEMBERS, 1));

        // Pull out the member IDs.
        Set<String> result = new HashSet<String>();
        while(members.hasNext()) {
            MemberInfo currMembers = members.next();
            if(currMembers.members == null) {
                continue;
            }

            for(Member member : currMembers.members) {
                result.add(member.getMemberId());
            }
        }

        // Return the result.
        return result;
    }

	/*
	 * (non-Javadoc)
	 * @see org.ohmage.bin.OhmletBin#updateOhmlet(org.ohmage.domain.Ohmlet)
	 */
	@Override
	public void updateOhmlet(
		final Ohmlet ohmlet)
		throws IllegalArgumentException {

		if(ohmlet == null) {
			throw new IllegalArgumentException("The ohmlet is null.");
		}

		// Create the query.
		// Limit the query only to this ohmlet.
		Query query = DBQuery.is(Ohmlet.JSON_KEY_ID, ohmlet.getId());
		// Ensure that the ohmlet has not been updated elsewhere.
		query =
			query
				.is(OhmageDomainObject.JSON_KEY_INTERNAL_VERSION,
					ohmlet.getInternalReadVersion());

		// Commit the update and don't return until the collection has heard
		// the result.
		WriteResult<Ohmlet, Object> result = COLLECTION.update(query, ohmlet);

		// Be sure that at least one document was updated.
		if(result.getN() == 0) {
			throw
				new InconsistentDatabaseException(
					"A conflict occurred. Please, try again.");
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ohmage.bin.OhmletBin#deleteOhmlet(java.lang.String)
	 */
	@Override
	public void deleteOhmlet(final String ohmletId)
		throws IllegalArgumentException {

		// Validate the input.
		if(ohmletId == null) {
			throw new IllegalArgumentException("The ohmlet ID is null.");
		}

		// Build the query
		QueryBuilder queryBuilder = QueryBuilder.start();

		// Add the ohmlet ID.
		queryBuilder.and(Ohmlet.JSON_KEY_ID).is(ohmletId);

		// Delete the ohmlet.
		COLLECTION.remove(queryBuilder.get());
	}
}