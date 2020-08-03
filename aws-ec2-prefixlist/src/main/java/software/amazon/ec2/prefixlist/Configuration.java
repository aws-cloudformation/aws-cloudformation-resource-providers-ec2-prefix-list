package software.amazon.ec2.prefixlist;

import java.util.Map;
import org.json.JSONObject;
import org.json.JSONTokener;

class Configuration extends BaseConfiguration {

    public Configuration() {
        super("aws-ec2-prefixlist.json");
    }
}
