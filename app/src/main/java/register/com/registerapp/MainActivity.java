package register.com.registerapp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TemplateManager templateManager = new TemplateManager();

        List<BaseState> list = templateManager.getStateList();
        for(int i=0; i<list.size(); i++){
            Log.e("xxxxxxxx MainActivity ", list.get(i).getName());
        }
    }
}
