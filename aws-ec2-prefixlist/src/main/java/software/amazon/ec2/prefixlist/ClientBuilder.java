package software.amazon.ec2.prefixlist;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;

public class ClientBuilder {
    private ClientBuilder() {
    }

    static AmazonEC2 getClient() {
        return AmazonEC2ClientBuilder.defaultClient();
    }
}
