package com.janssen1877.SOAP;

import nl.copernicus.niklas.transformer.*;
import nl.copernicus.niklas.transformer.context.ComponentContext;
import nl.copernicus.niklas.transformer.context.NiklasLogger;
import nl.copernicus.niklas.transformer.context.RoutingContext;
import org.apache.commons.text.StringEscapeUtils;

public class Message_encoder implements NiklasComponent<String, String>, NiklasLoggerAware, RoutingContextAware, ComponentContextAware {

    protected NiklasLogger log;
    protected RoutingContext rc;
    protected ComponentContext cc;


    @Override
    public String process(Header header, String payload) throws NiklasComponentException {
        String template = (String) cc.getProperties().get("template");
        if(template == null){
            throw new NiklasComponentException("Manditory property \"template\" not set up!");
        }
        String action = (String) cc.getProperties().get("action");

        Boolean escape = (Boolean) cc.getProperties().get("escape");
        if(escape == null){
            throw new NiklasComponentException("Manditory property \"escape\" not set up!");
        }

        if(action != null){
            header.setProperty("SoapAction", action);
        }

        if(escape) {
            return template.replace("{toReplace}", StringEscapeUtils.escapeXml11(payload));
        }else{
            return template.replace("{toReplace}",payload);
        }
    }


    @Override
    public void setLogger(NiklasLogger nl) {
        this.log = nl;
    }

    @Override
    public void setRoutingContext(RoutingContext routingContext) {
        this.rc = routingContext;
    }

    @Override
    public void setComponentContext(ComponentContext cc) {
        this.cc = cc;
    }
}

