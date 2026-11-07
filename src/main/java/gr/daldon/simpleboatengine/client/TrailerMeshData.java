package gr.daldon.simpleboatengine.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.InflaterInputStream;

/**
 * All 120 original Blockbench cuboids, including two independently spinning wheel
 * groups, from Aggelos_Blue_Boat_V2_Trailer_Round_Animated_Wheels.bbmodel.
 * Each row: min xyz, max xyz, element rotation xyz, pivot xyz, wheel flag, 6 face UVs.
 * Zlib packing keeps the original geometry and 64x64 texture atlas compact.
 */
public final class TrailerMeshData {
    private static final String MODEL = "eNrNmwly6zAIhi8ka7QvZ+m8+1/jAZK8JE3GTYzFtHXlduLvB2GEiPPzs1Qd1WLx4OmY4WDgxxlt1POXTfj959E/9ZOPoMrFebRogBZHo0eKUd6tV/nz8MgrYMqKS9pw0Mqg2QiH0mk289hmwQg4ePAeGoenRsM/w5Nx8BKjQsHXfzZEnGu4oGzFEaIMwKO/OEbwwhvHkVUMGAoIch/Mj8GQiOQ+8uWVoNSConNKn6XLMYBIig7kKhwYPCk80WB0UXRoABigVfH5NoZXGJUCXuGzIdIyhIOhSMfgXiJGB5w6r393IGhd75iPxgCNB2a+AwnT55pXF5doEp1qZ5YnYSVdVYNSqgfH1mazLSzAeOQlZtzOvnIwLzFbV/bGJWbbwt40ZsvCzjBuu2zaG8YdkURLvLB2a1OE2LImzQXQ7tq1YOOkDRPYKBsk4+9LVzUqcIKOVP7CX+nc60BVXGBb2jAAQuhLG5VaIT6tbV/aRhUVFjnBY0WAZ4RNYOLFpUEv3VzshaLF9Ovy1TUIzJPCBRJWye41BLqrPYcXXjHIZKEssdefgMENCxxS45TnsGsly7qofzQGJkJWZKSMxEyEAPRYc7QtWY98CnpvnrDtdoGyDK7xxRiwpe0hBhVF8EMX63VyVI6bbikmGJMjavFuv1Yv1mkfMfUYzDwQFteUsqckeAEakgANVYAG6ySIiBJEFAEinJUgIkgQkUVkKgki+PNlNHTdQGVoraTC1th36yDCvl43rmwQnFLi5UhJcqRUOVLerCn3a4mCtBQ5Wt4sNPdrCYK0ZEmJTpCWu7JuQRUOuVnDErXKc9Tv0T69XYkueN/pjIQQ52uoAvxgvQBHvE+rN4lwToAn3ievm0R4y+6J6qhjFUiFIxHBNRHUOMVu7UtPfNttPIPPbi7fhjBXgLNpsoBSWAUkumJq90EhBb6QqLZt88d3I+z+ndTPhtgZ7nvDbbs4NpCP/T2ereIZvp8tIM0WUGcLGPuviQridAVltoKxw5qoIExXkOeno+kKuDMi7dR2u7dRkj327jh3bqdkeCE6khAdVYiO1+vF3UKiFCFFiJDXi8jdQoIUIVlMNpMi5I68um7zx85/6wUc+3I8u/4z/N6UmyegzvbAaMdNVFBm+2A04iYqyLN9MFpwbArCsf8yOjLP/berey9n2GvzbQZ867zNoG9ttyn0tefGQKfu2q7jNppwrA23BT9uRJ+cKfSmWMUnWhP+oZLl9uInWYkGS3umrqVdaR3veT5ptaM65B2o+Mw4A9Po/tyLovGB6X97IPTrJ5+PrqxqTC3HTBJrN48PYcQyjzsmzeKByTKLjbjNYZtUvjn89x93aiOU";
    public static final float[][] PARTS = decode();
    private TrailerMeshData() {}

    private static float[][] decode() {
        try {
            byte[] compressed = Base64.getDecoder().decode(MODEL);
            byte[] decoded;
            try (var stream = new InflaterInputStream(new ByteArrayInputStream(compressed))) {
                decoded = stream.readAllBytes();
            }
            JsonArray all = JsonParser.parseString(
                new String(decoded, StandardCharsets.UTF_8)).getAsJsonArray();
            float[][] result = new float[all.size()][];
            for (int i = 0; i < all.size(); i++) {
                JsonArray values = all.get(i).getAsJsonArray();
                result[i] = new float[values.size()];
                for (int j = 0; j < values.size(); j++)
                    result[i][j] = values.get(j).getAsFloat();
            }
            return result;
        } catch (IOException | RuntimeException ex) {
            throw new IllegalStateException("Invalid fishing boat trailer model data", ex);
        }
    }
}
