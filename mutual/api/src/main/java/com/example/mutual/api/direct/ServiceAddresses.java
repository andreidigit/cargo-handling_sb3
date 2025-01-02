package com.example.mutual.api.direct;

public class ServiceAddresses {
    private final String compose;
    private final String product;
    private final String review;
    private final String recommend;

    public ServiceAddresses() {
        compose = null;
        product = null;
        review = null;
        recommend = null;
    }

    public ServiceAddresses(
            String compositeAddress,
            String productAddress,
            String reviewAddress,
            String recommendationAddress) {

        this.compose = compositeAddress;
        this.product = productAddress;
        this.review = reviewAddress;
        this.recommend = recommendationAddress;
    }

    public String getCompose() {
        return compose;
    }

    public String getProduct() {
        return product;
    }

    public String getReview() {
        return review;
    }

    public String getRecommend() {
        return recommend;
    }
}
