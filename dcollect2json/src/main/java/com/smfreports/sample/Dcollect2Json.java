package com.smfreports.sample;

import java.io.*;

import com.blackhillsoftware.dcollect.*;
import com.blackhillsoftware.json.*;
import com.blackhillsoftware.json.util.MultiLineArray;
import com.blackhillsoftware.smf.*;
import com.blackhillsoftware.zutil.io.TextRecordWriter;
import com.google.gson.Gson;
import com.ibm.jzos.ZFile;

public class Dcollect2Json {

    public static void main(String[] args) throws IOException 
    {
        EasySmfGsonBuilder gsonBuilder = new EasySmfGsonBuilder()
            .exclude("recordLength")
            .exclude("dculeng")            
            .includeUnsetFlags(false);

        EasySmfGsonBuilder prettyGsonBuilder = new EasySmfGsonBuilder()
            .exclude("recordLength")
            .exclude("dculeng")   
            .includeUnsetFlags(false)
            .setPrettyPrinting();

        try (
            VRecordReader reader = VRecordReader.fromDD("INPUT");

            OutDD dWriter  = new OutDD("OUTD" , gsonBuilder.createGson());
            OutDD aWriter  = new OutDD("OUTA" , gsonBuilder.createGson());
            OutDD vWriter  = new OutDD("OUTV" , gsonBuilder.createGson());
            OutDD mWriter  = new OutDD("OUTM" , gsonBuilder.createGson());
            OutDD bWriter  = new OutDD("OUTB" , gsonBuilder.createGson());
            OutDD cWriter  = new OutDD("OUTC" , gsonBuilder.createGson());
            OutDD tWriter  = new OutDD("OUTT" , gsonBuilder.createGson());
            OutDD dcWriter = new OutDD("OUTDC", gsonBuilder.createGson());
            OutDD scWriter = new OutDD("OUTSC", gsonBuilder.createGson());
            OutDD mcWriter = new OutDD("OUTMC", gsonBuilder.createGson());
            OutDD bcWriter = new OutDD("OUTBC", prettyGsonBuilder.createGson());
            OutDD sgWriter = new OutDD("OUTSG", gsonBuilder.createGson());
            OutDD vlWriter = new OutDD("OUTVL", gsonBuilder.createGson());
            OutDD agWriter = new OutDD("OUTAG", gsonBuilder.createGson());
            OutDD drWriter = new OutDD("OUTDR", gsonBuilder.createGson());
            OutDD lbWriter = new OutDD("OUTLB", gsonBuilder.createGson());
            OutDD cnWriter = new OutDD("OUTCN", gsonBuilder.createGson());
            OutDD aiWriter = new OutDD("OUTAI", prettyGsonBuilder.createGson());
            )
        {
            for (VRecord record : reader)
            {
                DcollectRecord dcollect = DcollectRecord.from(record);
                switch (dcollect.dcurctyp())
                {
                    case A:
                        aWriter.write(dcollect);
                        break;
                    case AG:
                        agWriter.write(dcollect);
                        break;
                    case AI:
                        aiWriter.write(dcollect);
                        break;
                    case B:
                        bWriter.write(dcollect);
                        break;
                    case BC:
                        bcWriter.write(dcollect);
                        break;
                    case C:
                        cWriter.write(dcollect);
                        break;
                    case CN:
                        cnWriter.write(dcollect);
                        break;
                    case D:
                        dWriter.write(dcollect);
                        break;
                    case DC:
                        dcWriter.write(dcollect);
                        break;
                    case DR:
                        drWriter.write(dcollect);
                        break;
                    case LB:
                        lbWriter.write(dcollect);
                        break;
                    case M:
                        mWriter.write(dcollect);
                        break;
                    case MC:
                        mcWriter.write(dcollect);
                        break;
                    case SC:
                        scWriter.write(dcollect);
                        break;
                    case SG:
                        sgWriter.write(dcollect);
                        break;
                    case T:
                        tWriter.write(dcollect);
                        break;
                    case V:
                        vWriter.write(dcollect);
                        break;
                    case VL:
                        vlWriter.write(dcollect);
                        break;
                    default:
                        break;   
                }
            }
        }
    }

    private static class OutDD implements Closeable
    {
        TextRecordWriter writer = null;
        MultiLineArray<DcollectRecord> jsonArray;

        public OutDD(String ddname, Gson gson) throws IOException 
        {
            jsonArray = new MultiLineArray<>(gson);
            if (ZFile.ddExists(ddname))
            {
                writer = TextRecordWriter.newWriterForDD(ddname);    
            }
        }

        public void write(DcollectRecord dcollectrecord) throws IOException
        {
            if (writer != null)
            {
                writer.writeLine(jsonArray.element(dcollectrecord));
            }
        }

        @Override
        public void close() throws IOException 
        {
            if (writer != null) 
            {
                writer.writeLine(jsonArray.endArray());
                writer.close();
                writer = null;
            }
        }
    }
}
