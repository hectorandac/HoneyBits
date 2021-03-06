package com.teko.honeybits.honeybits.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.teko.honeybits.honeybits.API.Authentication.LoginHandler;
import com.teko.honeybits.honeybits.CheckoutActivity;
import com.teko.honeybits.honeybits.R;
import com.teko.honeybits.honeybits.adapters.Cart.ProductAdapter;
import com.teko.honeybits.honeybits.models.Location;
import com.teko.honeybits.honeybits.models.Picture;
import com.teko.honeybits.honeybits.models.Price;
import com.teko.honeybits.honeybits.models.Product;
import com.teko.honeybits.honeybits.models.Shop;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CartFragment extends Fragment {
    private Button button;
    ProductAdapter adapter;
    Context context;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cart, container, false);

        context = getContext();



        RecyclerView.LayoutManager linearLayoutProducts = new LinearLayoutManager(context,
                LinearLayoutManager.VERTICAL, false);

        adapter = new ProductAdapter(context, (TextView) view.findViewById(R.id.total_amount));
        RecyclerView cart_holder = view.findViewById(R.id.cart_holder);
        cart_holder.setLayoutManager(linearLayoutProducts);
        cart_holder.setAdapter(adapter);



        view.findViewById(R.id.checkout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    openCheckout();
                }


        });



        if ((new LoginHandler(context).getToken()) != null) {
            new GetCartElements().execute();
        }
        return view;


    }

    public void openCheckout(){
        Intent intent = new Intent(getActivity().getApplication() , CheckoutActivity.class);
        startActivity(intent);

    }



    private class GetCartElements extends AsyncTask<Object, Void, ArrayList<Product>> {

        @Override
        protected ArrayList<Product> doInBackground(Object... objects) {
            try {

                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder()
                        .url("http://104.248.61.12/carts")
                        .get()
                        .addHeader("Authorization", new LoginHandler(context).getToken())
                        .build();

                Response response = client.newCall(request).execute();

                if (response.code() == 200) {

                    JSONObject cart = new JSONObject(response.body().string());

                    ArrayList<Product> products = new ArrayList<>();
                    JSONArray jArray = null;
                    jArray = cart.optJSONArray("products");
                    for (int i = 0; i < jArray.length(); i++){
                        JSONObject jObject = jArray.getJSONObject(i);
                        String id = jObject.getString("id");

                        Product tempProduct = objectInArray(products, id);
                        if (tempProduct != null) {
                            tempProduct.amount = tempProduct.amount + 1;

                        } else {
                            JSONObject jShop = jObject.getJSONObject("shop");
                            JSONObject jShopPicture = jShop.getJSONObject("shop_picture");
                            JSONObject jShopLogo = jShop.getJSONObject("shop_logo");
                            JSONObject jLocation = jShop.getJSONObject("map_location");
                            Shop shop = new Shop(
                                    jShop.getString("id"),
                                    jShop.getString("name"),
                                    jShop.getString("description"),
                                    new Picture(
                                            null,
                                            null,
                                            new Picture.SizeSources(
                                                    jShopPicture.getString("big"),
                                                    jShopPicture.getString("medium"),
                                                    jShopPicture.getString("thumb")
                                            )
                                    ),
                                    new Picture(
                                            null,
                                            null,
                                            new Picture.SizeSources(
                                                    jShopLogo.getString("big"),
                                                    jShopLogo.getString("medium"),
                                                    jShopLogo.getString("thumb")
                                            )

                                    ),
                                    new Location(
                                            jLocation.getString("place_id"),
                                            jLocation.getDouble("latitude"),
                                            jLocation.getDouble("longitude")
                                    ),
                                    jShop.getString("policy"),
                                    jShop.getString("raiting"),
                                    jShop.getBoolean("is_favorite")
                            );

                            JSONObject jPrice = jObject.getJSONObject("price");
                            Product product = new Product(
                                    jObject.getString("id"),
                                    jObject.getString("name"),
                                    jObject.getString("description"),
                                    shop,
                                    jObject.getBoolean("is_favorite"),
                                    new Price(
                                            jPrice.getString("raw"),
                                            jPrice.getString("formatted")
                                    )
                            );

                            JSONArray jPictures = jObject.getJSONArray("pictures");
                            for (int j = 0; j < jPictures.length(); j++) {
                                JSONObject jPicture = jPictures.getJSONObject(j);
                                JSONObject jUrls = jPicture.getJSONObject("urls");
                                Log.i("Test", jUrls.toString());
                                Picture picture = new Picture(
                                        jPicture.getString("id"),
                                        jPicture.getString("product"),
                                        new Picture.SizeSources(
                                                jUrls.getString("big"),
                                                jUrls.getString("medium"),
                                                jUrls.getString("thumb")
                                        )

                                );
                                product.getPicture().add(picture);
                            }

                            products.add(product);
                        }
                    }
                    return products;
                }
            } catch (JSONException | IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        private Product objectInArray(ArrayList<Product> arrayList, String id) {
            for (Product product : arrayList){
                if (product.getId().equals(id)) return product;
            }
            return null;
        }

        @Override
        protected void onPostExecute(ArrayList<Product> o) {
            super.onPostExecute(o);
            adapter.setProducts(o);
            adapter.notifyDataSetChanged();
        }
    }

}