package app.simple.inure.models;

import android.os.Parcel;
import android.os.Parcelable;

public class AppOp implements Parcelable {
    private String permission;
    private String id;
    private boolean enabled;
    private String time;
    private String duration;
    private String rejectTime;
    
    public AppOp(String permission, String id, boolean enabled, String time, String duration, String rejectTime) {
        this.permission = permission;
        this.id = id;
        this.enabled = enabled;
        this.time = time;
        this.duration = duration;
        this.rejectTime = rejectTime;
    }
    
    public AppOp() {
    }
    
    protected AppOp(Parcel in) {
        permission = in.readString();
        id = in.readString();
        enabled = in.readByte() != 0;
        time = in.readString();
        duration = in.readString();
        rejectTime = in.readString();
    }
    
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(permission);
        dest.writeString(id);
        dest.writeByte((byte) (enabled ? 1 : 0));
        dest.writeString(time);
        dest.writeString(duration);
        dest.writeString(rejectTime);
    }
    
    @Override
    public int describeContents() {
        return 0;
    }
    
    public static final Creator <AppOp> CREATOR = new Creator <AppOp>() {
        @Override
        public AppOp createFromParcel(Parcel in) {
            return new AppOp(in);
        }
        
        @Override
        public AppOp[] newArray(int size) {
            return new AppOp[size];
        }
    };
    
    public String getPermission() {
        return permission;
    }
    
    public void setPermission(String permission) {
        this.permission = permission;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public String getTime() {
        return time;
    }
    
    public void setTime(String time) {
        this.time = time;
    }
    
    public String getDuration() {
        return duration;
    }
    
    public void setDuration(String duration) {
        this.duration = duration;
    }
    
    public String getRejectTime() {
        return rejectTime;
    }
    
    public void setRejectTime(String rejectTime) {
        this.rejectTime = rejectTime;
    }
}
