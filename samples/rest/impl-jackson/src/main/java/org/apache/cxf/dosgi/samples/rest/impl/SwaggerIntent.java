package org.apache.cxf.dosgi.samples.rest.impl;

import org.apache.cxf.jaxrs.swagger.Swagger2Feature;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Created by nmw on 06-04-2017.
 */
@Component //
        (
                property = "org.apache.cxf.dosgi.IntentName=swagger" //
        )
public class SwaggerIntent implements Callable<List<Object>> {

    private String version = "";
    private String name = "";

    @Activate
    public void activate(BundleContext bundleContext) {
        Dictionary<String, String> headers = bundleContext.getBundle().getHeaders();
        version = headers.get("Bundle-Version");
        name = headers.get("Bundle-Name");


    }


    @Override
    public List<Object> call() throws Exception {
        Swagger2Feature swagger2Feature = new Swagger2Feature();
        swagger2Feature.setBasePath("/cxf/tasks/");
        swagger2Feature.setTitle(name);
        swagger2Feature.setDescription(name);
        swagger2Feature.setVersion(version);
        swagger2Feature.setScan(false);

//        swagger2Feature.setSupportSwaggerUi(true);
        swagger2Feature.setResourcePackage("org.apache.cxf.dosgi.samples.rest.impl");

        swagger2Feature.setPrettyPrint(true);


        return Arrays.asList(((Object )swagger2Feature));
    }

}

