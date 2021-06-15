package work.tinax.pandaroid;

import android.util.Log;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class UpdateChecker {
    private ExecutorService executor;
    private CloseableHttpClient client;
    private final Version version;

    public UpdateChecker(Version version) {
        this.version = version;
        executor = Executors.newSingleThreadExecutor();
        client = HttpClients.createDefault();
    }

    public Version getCurrentVersion() {
        return version;
    }

    public static final String UPDATES_JSON = "https://tinaxd.github.io/PandAroid-updates/updates.json";

    public Future<Boolean> isNewVersionAvailable() {
        HttpGet req = new HttpGet(UPDATES_JSON);
        return executor.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                try (CloseableHttpResponse res = client.execute(req)) {
                    String jsonString = EntityUtils.toString(res.getEntity());
                    ObjectMapper om = new ObjectMapper();
                    JsonNode tree = om.readTree(jsonString);
                    JsonNode versions = tree.get("versions");
                    List<Version> versionList = new ArrayList<>();
                    for (int i=0; versions.has(i); i++) {
                        JsonNode versionObj = versions.get(i);
                        String versionStr = versionObj.get("version").asText();
                        Version versionParsed = parseVersion(versionStr);
                        if (versionParsed != null) versionList.add(versionParsed);
                    }

                    for (Version v : versionList) {
                        Log.d("version info fetched", v.toString());
                    }

                    if (versionList.isEmpty()) return false;
                    return UpdateChecker.this.version.compareTo(versionList.stream().max(Version::compareTo).get()) < 1;
                } catch (IOException ex) {
                    ex.printStackTrace();
                    throw new UpdateCheckException("failed to contact the update site");
                } catch (Exception e) {
                    e.printStackTrace();
                    throw e;
                }
            }
        });
    }

    private static Version parseVersion(String versionStr) {
        String[] segs = versionStr.trim().split("\\.");
        if (segs.length != 3) {
            Log.i("update checker", "failed to parseVersion(length): " + versionStr);
            return null;
        }
        try {
            int major = Integer.parseInt(segs[0]);
            int minor = Integer.parseInt(segs[1]);
            int patch = Integer.parseInt(segs[2]);
            return new Version(major, minor, patch);
        } catch (NumberFormatException e) {
            Log.i("update checker", "failed to parseVersion(parseInt): " + versionStr);
            return null;
        }
    }
}
