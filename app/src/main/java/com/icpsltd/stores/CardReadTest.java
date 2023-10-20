package com.icpsltd.stores;

import androidx.appcompat.app.AppCompatActivity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Toast;

import com.credenceid.biometrics.ApduCommand;
import com.credenceid.biometrics.Biometrics;
import com.icpsltd.stores.biometricactivities.BiometricLogin;
import com.icpsltd.stores.biometricactivities.FingerPrint;
import com.icpsltd.stores.util.App;
import com.icpsltd.stores.util.Functions;
import com.icpsltd.stores.util.Variables;
import static com.credenceid.biometrics.Biometrics.ResultCode.FAIL;
import static com.credenceid.biometrics.Biometrics.ResultCode.INTERMEDIATE;
import static com.credenceid.biometrics.Biometrics.ResultCode.OK;

import java.math.BigInteger;
import java.util.Arrays;

public class CardReadTest extends AppCompatActivity {

    private static Biometrics.OnCardStatusListener onCardStatusListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_read_test);

        try{
            new OpenCardReaderAsync().execute();
        } catch (Exception e){
            e.printStackTrace();
        }

    }

    private class CardReading extends AsyncTask<Object, Boolean, Boolean> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Functions.Show_loader(CardReadTest.this, false, false);
        }

        @Override
        protected Boolean doInBackground(Object... objects) {
            String mAPDURead1k = "FF"         // MiFare Card
                    + "B0"                            // MiFare Card READ Command
                    + "00"                            // P1
                    + "00"                            // P2: Block Number
                    + "000400";                       // Number of bytes to read
            String mAPDURead4k = "FF"         // MiFare Card
                    + "B0"                            // MiFare Card READ Command
                    + "00"                            // P1
                    + "00"                            // P2: Block Number
                    + "001000";                       // Number of bytes to read
            /* Reads 2048 (2K) number of bytes from card. */
            String mAPDURead2k = "FF"         // MiFare Card
                    + "B0"                            // MiFare Card READ Command
                    + "00"                            // P1
                    + "00"                            // P2: Block Number
                    + "000800";                       // Number of bytes to read
            /* Reads 1024 (1K) number of bytes from card. */

            String mAPDUReadSpecialData = "FF"  // MiFare Card
                    + "B0"                              // MiFare Card READ Command
                    + "00"                              // P1
                    + "01"                              // P2: Block Number
                    + "00";                             // Number of bytes to read

            String mAPDUcustom = "FF"  // MiFare Card
                    + "CA"                              // MiFare Card READ Command
                    + "00"                              // P1 or "FF"
                    + "00"                              // P2: Block Number
                    + "00";                             // Number of bytes to read

            readCardAsync(mAPDUcustom);


            return true;
        }

        protected void onPostExecute() {
            Functions.cancel_loader();

        }

    }

    private void readCardAsync (String APDUcommand){
        App.BioManager.cardCommand(new ApduCommand(APDUcommand), false, (Biometrics.ResultCode resultcode, byte sw1, byte sw2, byte[] data) ->{
            if(OK == resultcode){
                //Toast.makeText(this, "sw1="+sw1, Toast.LENGTH_SHORT).show();
                //Toast.makeText(this, "sw1="+sw2, Toast.LENGTH_SHORT).show();
                //Toast.makeText(this, "data="+ Arrays.toString(data), Toast.LENGTH_SHORT).show();
                Toast.makeText(this, "HexString="+bytesToHexString(data), Toast.LENGTH_SHORT).show();
                BigInteger uidInt = new BigInteger(bytesToHexString(data), 16);
                Toast.makeText(this, "Integer Value: "+String.valueOf(uidInt), Toast.LENGTH_LONG).show();
            } else if (INTERMEDIATE == resultcode) {
                Toast.makeText(this, "INTERMEDIATE", Toast.LENGTH_SHORT).show();
            } else if (FAIL == resultcode) {
                Toast.makeText(this, "FAILED", Toast.LENGTH_SHORT).show();
            }

        });
    }



    private class OpenCardReaderAsync extends AsyncTask<Object, Boolean, Boolean> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            Functions.Show_loader(CardReadTest.this, false, true);
//            Functions.fetchAndSaveUsers(LoginActivity.this);
        }

        @Override
        protected Boolean doInBackground(Object... objects) {
            openCardReader();
            return true;
        }

        protected void onPostExecute() {
            Functions.cancel_loader();
            //Functions.arrangeFingers(finger_1,finger_2,finger_3,finger_4,cardDetails.getGhanaIdCardFpTemplateInfosList());

        }

    }

    public void openCardReaderMain() {
        //  SharedPreferences.Editor editor = Functions.getEditor(LoginActivity.this);
        App.BioManager.cardOpenCommand(new Biometrics.CardReaderStatusListener() {
            @Override
            public void onCardReaderOpen(Biometrics.ResultCode resultCode) {
                if (resultCode == Biometrics.ResultCode.OK) {

                    onCardStatusListener = new Biometrics.OnCardStatusListener() {
                        @Override
                        public void onCardStatusChange(String s, int prevState, int currentState) {
                            if (Variables.CARD_ABSENT == currentState) {
                                Functions.Show_Alert2(CardReadTest.this, "CARD ABSENT", "Place Card on top on the device");
                            } else {
                                Variables.mIsDocPresentOnEPassport = true;
                                Functions.hide_Alert2();

                                Toast.makeText(CardReadTest.this, "CARD DETECTED", Toast.LENGTH_SHORT).show();
                                new CardReading().execute();

                            }
                        }
                    };

                    App.BioManager.registerCardStatusListener(onCardStatusListener);
                    Functions.cancel_loader();
                } else {
                    Functions.cancel_loader();
                    Functions.Show_Alert2(CardReadTest.this, "Card Opening Error", "Error Opening Card Reader" + resultCode.toString());
                }
            }

            @Override
            public void onCardReaderClosed(Biometrics.ResultCode resultCode, Biometrics.CloseReasonCode closeReasonCode) {
                Functions.cancel_loader();
//                editor.putBoolean(Variables.is_card_reader_open, false);
//                editor.commit();
//                loginNotification.setText(Variables.restart_app);
//                btnLogin.setEnabled(false);
                //Functions.Show_Alert(LoginActivity.this,"Card Closed","Error Opening Card Reader");
            }
        });
    }

    private void openCardReader() {
        boolean isCardReaderOpened = Functions.getSharedPreference(CardReadTest.this).getBoolean(Variables.is_card_reader_open, false);

        if (isCardReaderOpened == false) {
            openCardReaderMain();
        } else {
            boolean cardConnection = App.BioManager.cardConnectSync(1000);
            if (cardConnection) {
                onCardStatusListener = new Biometrics.OnCardStatusListener() {
                    @Override
                    public void onCardStatusChange(String s, int prevState, int currentState) {
                        if (Variables.CARD_ABSENT == currentState) {
                            Functions.Show_Alert2(CardReadTest.this, "CARD ABSENT", "Place Card on top on the device");

                        } else {
                            Variables.mIsDocPresentOnEPassport = true;
                            Functions.hide_Alert2();

                        }
                    }
                };
                App.BioManager.registerCardStatusListener(onCardStatusListener);
                Functions.cancel_loader();
            } else {
                openCardReaderMain();
            }
        }


    }

    private String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    @Override
    protected void onResume() {
        super.onResume();

        onCardStatusListener = (String ATR,
                                int prevState,
                                int currentState) -> {
            /* If currentState is 1, then no card is present. */
            if (Variables.CARD_ABSENT == currentState) {
                Functions.Show_Alert2(CardReadTest.this, "CARD ABSENT", "Place Card on top on the device");
            } else {
                Variables.mIsDocPresentOnEPassport = true;
                Toast.makeText(CardReadTest.this, "CARD DETECTED", Toast.LENGTH_SHORT).show();
                new CardReading().execute();

                //CardDetails cardDetails = Functions.readGhanaCard(LoginActivity.this,"990409");
                //Log.d(Variables.TAG,cardDetails.toString());
                //readGhanaIdDocument(canNumber);
            }


        };

        App.BioManager.registerCardStatusListener(onCardStatusListener);
    }
}