package com.amazonaws.lambda.demo;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.event.S3EventNotification.S3EventNotificationRecord;
import com.amazonaws.services.s3.model.GetObjectRequest;
//import com.amazonaws.services.s3.model.S3Object; - Removed due to collision with rekognition s3object import
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClient;
import com.amazonaws.services.rekognition.model.DetectLabelsRequest;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.services.rekognition.model.Label;
import com.amazonaws.services.rekognition.model.S3Object;

/*
import com.amazonaws.services.rekognition.model.BoundingBox;
import com.amazonaws.services.rekognition.model.DetectLabelsRequest;
import com.amazonaws.services.rekognition.model.DetectLabelsResult;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.services.rekognition.model.Instance;
import com.amazonaws.services.rekognition.model.Label;
import com.amazonaws.services.rekognition.model.Parent;
import com.amazonaws.services.rekognition.model.S3Object;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import com.amazonaws.services.rekognition.model.AmazonRekognitionException;
*/

public class LambdaFunctionHandler implements RequestHandler<S3Event, String> {

	// Initialize s3 and rekognition
    private AmazonS3 s3 = AmazonS3ClientBuilder.standard().build();
    AmazonRekognition rekognitionClient = AmazonRekognitionClient.builder().build();

    public LambdaFunctionHandler() {}

    // Test purpose only.
    LambdaFunctionHandler(AmazonS3 s3) {
        this.s3 = s3;
    }

    @Override
    public String handleRequest(S3Event s3event, Context context) {
        S3EventNotificationRecord record = s3event.getRecords().get(0);

		String srcBucket = record.getS3().getBucket().getName();

		// Object key may have spaces or unicode non-ASCII characters.
		String srcKey = record.getS3().getObject().getUrlDecodedKey();

		String dstBucket = "secondscreendestbucket";
		// TODO some way to make each key unique
		String dstKey = "rekresults-" + srcKey.substring(0, (srcKey.length() - 4)) + ".txt";
		String labelNames = "";
		//String filepath = "output.txt";

		// Sanity check: validate that source and destination are different
		// buckets.
		if (srcBucket.equals(dstBucket)) {
		    System.out
		            .println("Destination bucket must not match source bucket.");
		    return "";
		}
		
		// perform detectlabels call on srcBucket
        Image imageToTag = new Image().withS3Object(new S3Object().withName(srcKey).withBucket(srcBucket));
        
		DetectLabelsRequest request = new DetectLabelsRequest()
                .withImage(imageToTag)
                .withMaxLabels(5)
                .withMinConfidence(77F);
		try {
            List<Label> labels = rekognitionClient.detectLabels(request).getLabels();
            //File file = new File(filepath);
            //FileWriter writer = new FileWriter(filepath);
            
            if (labels.isEmpty()) {
                System.out.println("No label is recognized!");
            } else {
                System.out.println("Detected labels for " + imageToTag.getS3Object().getName());
            }
            for (Label label : labels) {
                System.out.println(label.getName() + ": " + label.getConfidence().toString());
                //writer.write(label.getName() + ": " + label.getConfidence().toString() + System.lineSeparator());
                labelNames += (label.getName() + ": " + label.getConfidence().toString() + System.lineSeparator());
            }
            System.out.println(labelNames);
            InputStream stream = new ByteArrayInputStream(labelNames.getBytes(StandardCharsets.UTF_8));
            ObjectMetadata om = new ObjectMetadata();
            //TODO not sure about this
            om.setContentLength(labelNames.length());
         // Uploading to S3 destination bucket
            System.out.println("Writing to: " + dstBucket + "/" + dstKey);
            try {
                s3.putObject(dstBucket, dstKey, stream, om);
            }
            catch(AmazonServiceException e)
            {
                System.err.println(e.getErrorMessage());
                System.exit(1);
            }
			
		} catch (AmazonServiceException e) {
			e.printStackTrace();
		}
		return "ok";
    }
}