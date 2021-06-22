package com.sequenceiq.freeipa.api.v1.freeipa.upgrade.model;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image.ImageSettingsBase;

public class ImageInfoResponse extends ImageSettingsBase {

    private String imageName;

    private long created;

    private String date;

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public long getCreated() {
        return created;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    @Override
    public String toString() {
        return "ImageInfoResponse{" +
                "imageName='" + imageName + '\'' +
                ", created=" + created +
                ", date='" + date + '\'' +
                "} " + super.toString();
    }
}
