/*******************************************************************************
 * Copyright 2012 The Regents of the University of California
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.ohmage.query;

import java.util.TimeZone;
import java.util.UUID;

import org.ohmage.exception.DataAccessException;

/**
 * Interface to facilitate mocking concrete implementations for test cases. 
 * 
 * @author Joshua Selsky
 */
public interface IAnnotationQueries {
	/**
	 * Creates a survey response annotation.
	 * 
	 * @param annotationUuid  -- the id used to uniquely identify the annotation
	 * @param client          -- the software client making the request
	 * @param time            -- the milliseconds since the UNIX epoch
	 * @param timezone        -- the timezone of the system making the request
	 * @param annotationText  -- the annotation text
	 * @param surveyId        -- the UUID of the survey response
	 * @throws DataAccessException if an error occurs
	 */
	void createSurveyResponseAnnotation(
		UUID annotationUuid,
		String client,
		Long time,
		TimeZone timezone,
		String annotationText,
		UUID surveyId
	) throws DataAccessException;
	
}