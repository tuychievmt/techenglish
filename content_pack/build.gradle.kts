plugins {
    alias(libs.plugins.android.asset.pack)
}

assetPack {
    packName.set("content_pack")
    dynamicDelivery {
        deliveryType.set("install-time")
    }
}
