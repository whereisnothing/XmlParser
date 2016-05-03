package com.chenxu.xmlparser;

import android.app.Application;
import android.test.ApplicationTestCase;
import android.util.Log;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ApplicationTest extends ApplicationTestCase<Application> {
    public void testXml(){
        int i = 5;
        XmlParser xmlParser = new XmlParser(getContext(), "");
        xmlParser.setFileNameInAssetsFolder("xml.txt");
        XmlParser.ResultBean resultBean = xmlParser.xmlStringToObject();
        Log.i("chenxu", xmlParser.objectToXmlString());
    }
    public ApplicationTest() {
        super(Application.class);
    }
}