package headspin.io;
import headspin.io.hsapi.HsApi;
import static headspin.io.hsapi.GlobalVar.*;
public class PostProcessing {
    public static void main(String[] args) {
        HsApi d = new HsApi();
        d.get_session_ids();
        d.upload_image();
        for (String session_id:session_ids) {
            d.Image_Analysis(session_id);
            d.text_analysis(session_id,"image-match-result","image match result");
            d.audio_analysis(session_id,d.upload_audio());
            break;
        }

    }
}
