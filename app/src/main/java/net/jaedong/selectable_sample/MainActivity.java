package net.jaedong.selectable_sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import net.jaedong.textview.SelectableView;
import net.jaedong.textview.view.SelectableListener;

public class MainActivity extends AppCompatActivity {

    private TextView emptyBoxBtn;
    private SelectableView selectableView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        selectableView = (SelectableView) findViewById(R.id.selectableView);
        selectableView.setActivity(this);
        selectableView.setText(getString(R.string.sample_text));
        selectableView.addOnSaveClickListener(new SelectableListener() {
            @Override
            public void selectedText(String text) {
                Log.e("TAG", text);
                Toast.makeText(MainActivity.this, text, Toast.LENGTH_LONG).show();
            }
        });

        emptyBoxBtn = (TextView) findViewById(R.id.emptyBoxBtn);
        emptyBoxBtn.setTag(0);
        emptyBoxBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int type = (int) view.getTag();
                if (type == 0) {
                    emptyBoxBtn.setBackgroundResource(R.drawable.selector_cancel_btn);
                    selectableView.selectAll();
                    emptyBoxBtn.setTag(1);
                } else {
                    emptyBoxBtn.setBackgroundResource(R.drawable.selector_emptybox);
                    selectableView.hideCursor();
                    emptyBoxBtn.setTag(0);
                }
            }
        });
    }
}
