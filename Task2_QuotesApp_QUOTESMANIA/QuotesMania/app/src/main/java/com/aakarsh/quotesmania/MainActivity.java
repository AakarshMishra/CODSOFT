package com.aakarsh.quotesmania;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import androidx.appcompat.app.AppCompatActivity;



import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private TextView quoteTextView;
    private ImageView refreshButton;
    private ImageView shareButton;
    private ImageView favoriteButton;
    private ImageView imageView;
    private LinearLayout favoriteLayout, developerLayout;

    private QuoteApiService quoteApiService;
    private String currentQuote = "", currentAuthor = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        quoteTextView = findViewById(R.id.quoteTextView);
        refreshButton = findViewById(R.id.refreshButton);
        shareButton = findViewById(R.id.shareButton);
        favoriteButton = findViewById(R.id.favoriteButton);
        imageView = findViewById(R.id.viewFavorite);
        favoriteLayout = findViewById(R.id.favoriteLayout);


        favoriteLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, FavouriteQuotesActivity.class));
            }
        });

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.quotable.io/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        quoteApiService = retrofit.create(QuoteApiService.class);

        refreshQuote();

        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshQuote();
            }
        });

        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareCurrentQuote();
            }
        });

        favoriteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addToFavorites();
            }
        });
    }

    private void refreshQuote() {
        Call<QuoteResponse> call = quoteApiService.getRandomQuote();
        call.enqueue(new Callback<QuoteResponse>() {
            @Override
            public void onResponse(Call<QuoteResponse> call, Response<QuoteResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    QuoteResponse quoteResponse = response.body();
                    currentQuote = quoteResponse.getContent();
                    currentAuthor = quoteResponse.getAuthor();


                    quoteTextView.setText("'"+currentQuote + "'"+"\n\n- " + currentAuthor);
                }
            }

            @Override
            public void onFailure(Call<QuoteResponse> call, Throwable t) {
                // Handle failure (e.g., show an error message)
            }
        });
    }

    private void shareCurrentQuote() {
        String shareContent = currentQuote + "\n\n- " + currentAuthor;
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareContent);
        startActivity(Intent.createChooser(shareIntent, "Share via"));
    }

    private void addToFavorites() {
        String quote = currentQuote;
        String author = currentAuthor;
        String timestamp = getCurrentTimestamp();

        QuoteDatabaseHelper dbHelper = new QuoteDatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Query to check if the quote already exists
        String selection = QuoteDatabaseHelper.COLUMN_QUOTE + " = ? AND " + QuoteDatabaseHelper.COLUMN_AUTHOR + " = ?";
        String[] selectionArgs = { quote, author };

        Cursor cursor = db.query(
                QuoteDatabaseHelper.FAVORITES_TABLE,
                null, // Return all columns
                selection,
                selectionArgs,
                null,
                null,
                null
        );

        if (cursor != null && cursor.getCount() > 0) {
            // Quote already exists
            Toast.makeText(this, "Quote already in favorites", Toast.LENGTH_SHORT).show();
        } else {
            // Quote does not exist, proceed with insertion
            ContentValues values = new ContentValues();
            values.put(QuoteDatabaseHelper.COLUMN_QUOTE, quote);
            values.put(QuoteDatabaseHelper.COLUMN_AUTHOR, author);
            values.put(QuoteDatabaseHelper.COLUMN_TIMESTAMP, timestamp);

            long newRowId = db.insert(QuoteDatabaseHelper.FAVORITES_TABLE, null, values);

            if (newRowId != -1) {
                Toast.makeText(this, "Quote added to favorites", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to add quote to favorites", Toast.LENGTH_SHORT).show();
            }
        }

        if (cursor != null) {
            cursor.close();
        }
        db.close();
    }


    private String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }
}