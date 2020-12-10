package com.example.taskete.data

import android.os.Parcelable
import com.j256.ormlite.dao.ForeignCollection
import com.j256.ormlite.field.DataType
import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.field.ForeignCollectionField
import com.j256.ormlite.table.DatabaseTable
import kotlinx.android.parcel.Parcelize

@Parcelize
@DatabaseTable(tableName = "Users")
class User(
        @DatabaseField(id = true)
        var id: Int? = null,
        @DatabaseField
        var username: String,
        @DatabaseField
        var mail: String,
        @DatabaseField
        var password: String,
        @DatabaseField
        var avatar: String? = null, //Store filepath or image
        @ForeignCollectionField(eager=false) //eager=false need to refresh before modifying the tasks field
        var tasks: ForeignCollection<Task>? = null
) : Parcelable {

    //TODO: Check how to store images
    //TODO: Encrypt the password
    constructor() : this(null, "", "", "", null)

}

