package com.ylz.seniorlearning.spring.beans;

/**
 * Created by Jonas on 2017/8/30.
 */
public class ProductBean {
    private long productId;
    private String productName;
    private int productType;
    private String image;
    private String tips;
    private String standardPrice;
    private String youhui;
    private int providerTypeId;

    public long getProductId() {
        return productId;
    }

    public void setProductId(long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public int getProductType() {
        return productType;
    }

    public void setProductType(int productType) {
        this.productType = productType;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getTips() {
        return tips;
    }

    public void setTips(String tips) {
        this.tips = tips;
    }

    public String getStandardPrice() {
        return standardPrice;
    }

    public void setStandardPrice(String standardPrice) {
        this.standardPrice = standardPrice;
    }

    public String getYouhui() {
        return youhui;
    }

    public void setYouhui(String youhui) {
        this.youhui = youhui;
    }

    public int getProviderTypeId() {
        return providerTypeId;
    }

    public void setProviderTypeId(int providerTypeId) {
        this.providerTypeId = providerTypeId;
    }
}
