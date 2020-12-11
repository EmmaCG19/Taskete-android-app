package com.example.taskete.data

import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.RequiresApi
import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.field.ForeignCollectionField
import com.j256.ormlite.table.DatabaseTable

@DatabaseTable(tableName = "Users")
class User(
        @DatabaseField(id = true)
        var id: Int? = null,
        @DatabaseField
        var username: String,
        @DatabaseField(unique = true)
        var mail: String,
        @DatabaseField
        var password: String,
        @DatabaseField
        var avatar: String? = null, //Store filepath or image
        @ForeignCollectionField(eager = false) //eager=false need to refresh before modifying the tasks field
        var tasks: Collection<Task>
) : Parcelable {

    constructor() : this(null, "", "", "", null, arrayListOf<Task>())

    constructor(parcel: Parcel) : this(
            parcel.readValue(Int::class.java.classLoader) as? Int,
            parcel.readValue(String::class.java.classLoader) as String,
            parcel.readValue(String::class.java.classLoader) as String,
            parcel.readValue(String::class.java.classLoader) as String,
            parcel.readString(),
            arrayListOf<Task>().apply {
                parcel.readList(this, Task::class.java.classLoader)
            })

    //TODO: CIRCULAR REFERENCE WITH WRITE PARCELABLE
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeValue(id)
        parcel.writeString(username)
        parcel.writeString(mail)
        parcel.writeString(password)
        parcel.writeString(avatar)

        arrayListOf<Task>().also { it ->
            for (task in tasks) {
                it.add(task)
            }
            parcel.writeParcelableList(it, flags)
        }
    }

    override fun describeContents() = 0

    companion object CREATOR : Parcelable.Creator<User> {
        override fun createFromParcel(parcel: Parcel): User {
            return User(parcel)
        }

        override fun newArray(size: Int): Array<User?> {
            return arrayOfNulls(size)
        }
    }

}

//TODO: Check how to store images
//TODO: Encrypt the password