PicasaUploadSample
==================

This sample code for android shows how to implement uploading photo to Picasa.

Android で Picasa に写真をアップロードするサンプルが無かった（※）ので、東北TECH道場での説明用に作成しました。

google-api-java-client のライブラリを使用して、
Picasa の API に関する部分は
https://code.google.com/p/google-api-java-client/source/browse?repo=samples#hg/picasa-cmdline-sample
を参考に、
Android に関する部分は
http://samples.google-api-java-client.googlecode.com/hg/tasks-android-sample/instructions.html
を参考にしています。

また、https://developers.google.com/picasa-web/docs/2.0/developers_guide_protocol#AddTags を元に、写真にタグを付ける機能を追加しています。

※ あるにはあったのですが、古いもので AsyncTask 等も使っておらず現在の環境には適切なものではなかったので。。。
