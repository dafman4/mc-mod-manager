module mc.mod.manager.api {

	requires org.slf4j;
	requires ch.qos.logback.classic;
	requires org.apache.httpcomponents.httpclient;
	requires com.fasterxml.jackson.core;
	requires com.fasterxml.jackson.databind;
	requires my.utilities;

	exports com.squedgy.mcmodmanager.api;
}