package br.ufc.prograd.cgpa.euufc;

import android.app.ProgressDialog;
import android.arch.persistence.room.Room;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;

import android.os.AsyncTask;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import br.com.jansenfelipe.androidmask.MaskEditTextChangedListener;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity {

    public static final String LOGIN_ARQUIVO = "ArquivoLogin";
    /**
     * Id to identity READ_CONTACTS permission request.
     */
//    private static final int REQUEST_READ_CONTACTS = 0;

    /**
     * A dummy authentication store containing known user names and passwords.
     * TODO: remove after connecting to a real authentication system.
     */
//    private static final String[] DUMMY_CREDENTIALS = new String[]{
//            "foo@example.com:hello", "bar@example.com:world"
//    };
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;

    // UI references.
    TextView loginCpf;
    TextView loginMatricula;
    boolean click = true;

    String resultado = "";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);



        SharedPreferences infoLogin = getSharedPreferences(LOGIN_ARQUIVO,0);
        boolean autenticado = infoLogin.getBoolean("autenticado",false);
        if(autenticado == true){
            Intent intent = new Intent(getApplicationContext(), Home.class);
            startActivity(intent);
            this.finish();
        }

        loginCpf = (TextView) findViewById(R.id.textCpf);
       // loginMatricula = (TextView) findViewById(R.id.textMatricula);

        EditText cpf = (EditText) findViewById(R.id.textCpf);
        MaskEditTextChangedListener maskCPF = new MaskEditTextChangedListener("###.###.###-##", cpf);

        cpf.addTextChangedListener(maskCPF);

    }


    public void login(View view){


        if(loginCpf.getText().toString().replaceAll("[^0-9]", "").length() == 11 && click == true) {
            click = false;
            // Instantiate the RequestQueue.
            RequestQueue queue = Volley.newRequestQueue(this);
            String url = "http://200.19.177.136/api/alunos/" + loginCpf.getText().toString().replaceAll("[^0-9]", "");
            ;

// Request a string response from the provided URL.
            StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            // Display the first 500 characters of the response string.
                            resultado = response.toString();
                            mAuthTask = new UserLoginTask(loginCpf.getText().toString().replaceAll("[^0-9]", ""), "0");
                            mAuthTask.execute();
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Toast.makeText(getApplicationContext(), "Não conseguimos nos conectar ao servidor", Toast.LENGTH_LONG).show();
                    click = true;
                }
            });

            // Add the request to the RequestQueue.
            queue.add(stringRequest);
        }else{
            loginCpf.setError(getString(R.string.cpf_incomplete));
            click = true;
        }

    }



    private boolean isEmailValid(String email) {
        //TODO: Replace this with your own logic
        return email.contains("@");
    }

    private boolean isPasswordValid(String password) {
        //TODO: Replace this with your own logic
        return password.length() > 4;
    }

    private boolean isCpfValid (String cpf){
        return cpf.length() >= 4;
    }


    public void saveInfoLogin(String cpf, String nome, int id){
        SharedPreferences infoLogin = getSharedPreferences(LOGIN_ARQUIVO,0);
        SharedPreferences.Editor editor = infoLogin.edit();
        editor.putBoolean("autenticado",true);
        editor.putString("cpf",cpf);
        editor.putString("nome",nome );
        editor.putInt("id",id);
        editor.commit();

    }

    public void finalizaActivity(){
        this.finish();
    }

    public class UserLoginTask extends AsyncTask<Void, Void, User> {

        private final String mCpf;
        private final String mMatricula;
        private ProgressDialog load;


        UserLoginTask(String cpf, String matricula) {
            mCpf = cpf;
            mMatricula = matricula;

        }

        @Override
        protected User doInBackground(Void... params) {
            // TODO: attempt authentication against a network service.

            AppDatabase db = Room.databaseBuilder(getApplicationContext(),
                    AppDatabase.class, "database-name").build();

            if(resultado.length()>5){
                try{
                    JSONObject jsonObj = new JSONObject(resultado);
                    //JSONObject usuario = jsonObj.getJSONObject("usuario");
                    String nome = jsonObj.getString("nome");
                    String cpf = mCpf;
                    //int matricula = usuario.getInt("matricula");
                    User novo_usuario = new User(0, nome, cpf);
                    //Adiciona novo usuario somente se o mesmo não existe no BD
                    if(db.userDao().findByName(nome) == null) {
                        db.userDao().insertAll(novo_usuario);
                    }
                    return db.userDao().findByName(nome);
                }catch(Exception e){
                    e.printStackTrace();
                }
            }

            User novo_usuario = new User(0, "","");

            return novo_usuario;

        }

        @Override
        protected void onPostExecute(final User user) {
            mAuthTask = null;
            //showProgress(false);
            load.dismiss();

            if (mCpf.equals(user.getCpf()) &&  mMatricula.equals(String.valueOf(user.getMatricula()))) {

                click = false;
                saveInfoLogin(user.getCpf(),user.getNome(), user.getId());
                Intent intent = new Intent(getApplicationContext(), OnBoardActivity.class);
                startActivity(intent);
                finalizaActivity();
            } else {
                click = true;
                //loginMatricula.setError(getString(R.string.error_user_not_found));
                loginCpf.setError(getString(R.string.error_user_not_found));
            }

        }


        @Override
        protected void onPreExecute(){
            load = ProgressDialog.show(LoginActivity.this, "Por favor Aguarde ...", "Recuperando Informações do Servidor...");

        }
    }

}

