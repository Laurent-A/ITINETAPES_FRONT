package com.app.itinetape;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class EtapeActivity extends AppCompatActivity {
    private static final Object REQUEST_TAG = new Object();
    private RequestQueue requestQueue;
    private TextView nbFavori;
    private TextView titreEtape;
    private TextView descriptionEtape;
    private Integer nbFavoriEtapeActuelle;
    private Integer numeroEtapeActuelle;
    private String titreEtapeActuelle;
    private String descriptionEtapeActuelle;
    private Object itineraire;
    private Boolean favori = false;
    private String favoriInterne;
    public static final String PREFS_NAME = "MonEtapeCheri";
    private String url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_etape);

        titreEtape = findViewById(R.id.titreEtape);
        descriptionEtape = findViewById(R.id.description);
        nbFavori = findViewById(R.id.nbFavori);

        requestQueue = Volley.newRequestQueue(this);
        this.search(titreEtape);
        ajoutFavori();
        readData();
    }

    @Override
    protected void onStop() {
        super.onStop();
        cancelRequest();
    }

    private void cancelRequest() {
        if (requestQueue != null) {
            requestQueue.cancelAll(REQUEST_TAG);
        }
    }

    public void search(View view) {
        String titreEtapeActuelle = this.titreEtape.getText().toString();
        if (titreEtapeActuelle.isEmpty()) {
            return;
        }
        if (titreEtapeActuelle.length() == 1) {
            titreEtapeActuelle = "0" + titreEtapeActuelle;
        }
        sendRequest(titreEtapeActuelle);
    }

    private void sendRequest(String titreEtapeActuelle) {
        cancelRequest();
        // Je récupère le qr code via mon intent
        Uri intentData = getIntent().getData();
        // je le met au format string
        String urlQR = String.format(intentData.toString());
        // je le split dans un tableau
        String[] urlStorage = urlQR.split("'");
        // je récupère l'url au bon format
        String URL_PATTERN = urlStorage[1];
        titreEtape.setText("Chargement...");
        url = String.format(URL_PATTERN, titreEtapeActuelle);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        fillEtapeResultat(response);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if(error.networkResponse != null
                        && error.networkResponse.statusCode == 404) {
                    Toast.makeText(getApplicationContext(), "Pas d'étape trouvé. Veuillez vérifier le qrCode saisi.", Toast.LENGTH_SHORT).show();
                    titreEtape.setText("Erreur 404");
                } else {
                    titreEtape.setText("Erreur 1 : " + error);
                    Toast.makeText(getApplicationContext(), "Erreur 1 : " + error, Toast.LENGTH_SHORT).show();
                }
            }
        });
        request.setTag(REQUEST_TAG);
        requestQueue.add(request);
    }

    private void fillEtapeResultat(JSONObject jsonObject) {
        try {
            titreEtapeActuelle = jsonObject.getString("nom");
            descriptionEtapeActuelle = jsonObject.getString("description");
            nbFavoriEtapeActuelle = jsonObject.getInt("totalfavori");
            numeroEtapeActuelle = jsonObject.getInt("ordreEtape");
            itineraire = jsonObject.getJSONObject("itineraire");

            String msgNom = String.format("Etape n°%s : %s",numeroEtapeActuelle, titreEtapeActuelle);
            String msgDescription = String.format("%s", descriptionEtapeActuelle);
            String msgFavori = String.format("%s", nbFavoriEtapeActuelle);

            titreEtape.setText(msgNom);
            descriptionEtape.setText(msgDescription);
            nbFavori.setText(msgFavori);
        } catch (JSONException e) {
            titreEtape.setText("Erreur 2 : " + e.getMessage());
        }
    }

    // Requete de modification des données
    private void putRequest() throws JSONException {
        if(favori){
            nbFavoriEtapeActuelle = nbFavoriEtapeActuelle-1;
        }else {
            nbFavoriEtapeActuelle = nbFavoriEtapeActuelle + 1;
        }
        JSONObject requestPayload = new JSONObject();
        requestPayload.put("totalfavori", nbFavoriEtapeActuelle);
        requestPayload.put("nom", titreEtapeActuelle);
        requestPayload.put("description", descriptionEtapeActuelle);
        requestPayload.put("ordreEtape", numeroEtapeActuelle);
        requestPayload.put("itineraire", itineraire);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.PUT, url, requestPayload,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d("Response", String.valueOf(response));
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                //
            }
        });
        this.requestQueue.add(request);
    }


    // Ajout ou retrait d'un favori appel post
    public void ajoutFavori() {
        final ImageView ajoutfavori = findViewById(R.id.choixFavori);
        ajoutfavori.setClickable(true);
        ajoutfavori.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(!favori){
                    try {
                        putRequest();
                        Toast.makeText(getApplicationContext(), "Merci petit pédestre !", Toast.LENGTH_SHORT).show();
                        ajoutfavori.setImageDrawable(getResources().getDrawable(android.R.drawable.star_big_on));
                        favori = true;
                        saveInterne();
                    } catch (JSONException | IOException e) {
                        Toast.makeText(getApplicationContext(), "Comprend pas...", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    try {
                        putRequest();
                        Toast.makeText(getApplicationContext(), "On a bien compris que tu n'aimais plus cette étape", Toast.LENGTH_SHORT).show();
                        ajoutfavori.setImageDrawable(getResources().getDrawable(android.R.drawable.star_big_off));
                        favori = false;
                        saveInterne();
                    } catch (JSONException | IOException e) {
                        Toast.makeText(getApplicationContext(), "Comprend pas...", Toast.LENGTH_SHORT).show();
                    }
                }

            }
        });
    }

    // Stockage choix du favori en interne
    private void saveInterne() throws IOException {
        FileOutputStream out = this.openFileOutput(PREFS_NAME, MODE_PRIVATE);
        if (favori){
            favoriInterne = "true" + "/";

        }else{
            favoriInterne = "false" + "/";
        }
        try {
            String etape = String.format("%s", numeroEtapeActuelle);
            out.write(favoriInterne.getBytes());
            out.write(etape.getBytes());
            out.close();
        } catch (Exception e) {
            Toast.makeText(this,"Error:"+ e.getMessage(),Toast.LENGTH_SHORT).show();
        }
    }

    // Lecture
    private void readData() {
        final ImageView ajoutfavori = findViewById(R.id.choixFavori);
        try {
            FileInputStream input = openFileInput(PREFS_NAME);
            int value;
            StringBuffer lu = new StringBuffer();
            while((value = input.read()) != -1){
                String resultat = lu.append((char)value).toString();
                if (resultat.contentEquals("true")){
                    favori = true;
                    //Toast.makeText(EtapeActivity.this, "Interne : " + lu.toString(), Toast.LENGTH_SHORT).show();
                    ajoutfavori.setImageDrawable(getResources().getDrawable(android.R.drawable.star_big_on));
                } else {
                    //Toast.makeText(EtapeActivity.this, "Rien avoir : ",Toast.LENGTH_SHORT).show();
                    ajoutfavori.setImageDrawable(getResources().getDrawable(android.R.drawable.star_big_off));
                }
            }
            Toast.makeText(EtapeActivity.this, "Interne : " + lu.toString(), Toast.LENGTH_SHORT).show();
            if(input != null)
                input.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
