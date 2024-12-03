package za.ac.nmmu.lift_club.util;

import android.util.JsonWriter;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.StringWriter;

/**
 * Created by s210036575 on 2015-08-13.
 */
public class Car {
    public String vrn = "", manufacturer = "", model = "", colour = "", pic_id = "";
    public boolean hasBoot;
    public byte nrSeats = 0;

    public Car(String json) throws JSONException {
        JSONObject jsonObject = new JSONObject(json);

        vrn = jsonObject.getString("vrn");
        manufacturer = jsonObject.getString("manufacturer");
        model = jsonObject.getString("model");
        colour = jsonObject.getString("colour");
        hasBoot = jsonObject.getBoolean("hasBoot");
        nrSeats = (byte)jsonObject.getInt("maxSeats");
        pic_id = jsonObject.getString("pic_id");
    }

    public String toJson()
    {
        StringWriter sw =  new StringWriter();
        JsonWriter writer = new JsonWriter(sw);

        try {
            writer.beginObject().name("vrn").value(vrn).name("manufacturer").value(manufacturer)
                    .name("model").value(model).name("colour").value(colour).name("hasBoot").value(hasBoot)
                    .name("maxSeats").value(nrSeats).name("pic_id").value(pic_id).endObject().flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return sw.toString();
    }

}
