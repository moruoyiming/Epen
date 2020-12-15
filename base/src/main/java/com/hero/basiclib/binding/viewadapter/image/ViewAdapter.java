package com.hero.basiclib.binding.viewadapter.image;

import android.widget.ImageView;

import androidx.databinding.BindingAdapter;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

/**
 * Created by goldze on 2017/6/18.
 */
public final class ViewAdapter {
    @BindingAdapter(value = {"url", "options"}, requireAll = false)
    public static void setImageUri(ImageView imageView, String url, RequestOptions options) {
        if (url!=null) {
            if(options == null){
                Glide.with(imageView.getContext())
                        .load(url)
                        .into(imageView);
            }else {
                //使用Glide框架加载图片
                Glide.with(imageView.getContext())
                        .load(url)
                        .apply(options)
                        .into(imageView);
            }
        } else {
            if(options == null){
                Glide.with(imageView.getContext())
                        .load("")
                        .into(imageView);
            }else {
                //使用Glide框架加载图片
                Glide.with(imageView.getContext())
                        .load("")
                        .apply(options)
                        .into(imageView);
            }
        }
    }

    @BindingAdapter(value = {"urlRes", "options"}, requireAll = false)
    public static void setLocalImageUri(ImageView imageView, int urlRes, RequestOptions options) {
        //使用Glide框架加载图片

        if(options == null){
            Glide.with(imageView.getContext())
                    .load(urlRes)
                    .into(imageView);
        }else {
            //使用Glide框架加载图片
            Glide.with(imageView.getContext())
                    .load(urlRes)
                    .apply(options)
                    .into(imageView);
        }
    }

//    @BindingAdapter(value = {"url", "options"}, requireAll = false)
//    public static void setRoundImageUri(ImageView imageView, String url, RequestOptions options) {
//        if (!TextUtils.isEmpty(url)) {
//            //使用Glide框架加载图片
//            Glide.with(imageView.getContext())
//                    .load(url)
//                    .apply(options)
//                    .into(imageView);
//        }
//    }
}

