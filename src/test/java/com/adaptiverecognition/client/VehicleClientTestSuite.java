package com.adaptiverecognition.client;

import org.junit.platform.suite.api.SelectMethod;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

@Suite
@SuiteDisplayName("Vehicle Client Test Suite")
@SelectMethod(type = VehicleClientTest.class, name = "testApp")
public class VehicleClientTestSuite {

}
