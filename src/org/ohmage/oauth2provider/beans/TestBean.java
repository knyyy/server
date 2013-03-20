package org.ohmage.oauth2provider.beans;

import java.io.Serializable;

/**
 * created by Faisal on 3/20/13 1:03 AM
 */
public class TestBean implements Serializable {
    private String name;

    public TestBean() {

    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
