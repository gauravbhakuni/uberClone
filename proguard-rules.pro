# Keep the entire Google Play services library
-keep class com.google.android.gms.** { *; }

# Keep the Maps API
-keep class com.google.android.gms.maps.** { *; }

# Keep the location services API
-keep class com.google.android.gms.location.** { *; }

# Keep the GoogleApiClient class
-keep class com.google.android.gms.common.api.GoogleApiClient { *; }

# Keep the Result and Status classes
-keep class com.google.android.gms.common.api.Result { *; }
-keep class com.google.android.gms.common.api.Status { *; }
