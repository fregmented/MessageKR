# MessagingKR

[ ![Download](https://api.bintray.com/packages/seyriz/kudryavka/MessageKR/images/download.svg) ](https://bintray.com/seyriz/kudryavka/MessageKR/_latestVersion)

MessageKR is SMS and MMS sender for korean Telecom providers.

This project inspired by [AndroidMMS](https://github.com/kakopappa/AndroidMMS)

MessageKR은 한국 통신사를 위한 SMS/MMS 래퍼입니다.

## Usage

```groovy
dependencies {
    ...
    compile 'me.kudryavka:MessageKR:1.0.1'
    ...
}
```


SMS와 MMS를 전송하기 위해 필요한 최소한의 권한입니다.

```xml
<uses-permission android:name="android.permission.SEND_SMS" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.WRITE_SMS" />
<uses-permission android:name="android.permission.CHANGE_NETWORK_STATE" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.WRITE_SETTINGS"/>
```

또한 안드로이드 6.0부터는 시스템 설정을 쓰기 위해 사용자에게 직접 권한 설정을 요청해야 합니다.

```java
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(this)) {
        new AlertDialog.Builder(this)
            .setMessage("안드로이드 6.0(마시멜로우)부터 애플리케이션은 사진 등이 포함된 MMS를 자동으로 보낼 수 없습니다.\n
                         MMS를 보내려면 설정화면에서 직접 허가해 주셔야 합니다.\n
                         허가하지 않아도 앱 구동에는 문제가 없지만, 사진이 포함된 메세지를 보내는 기능이 비활성화 됩니다.")
            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    try {
                        startActivity(intent);
                    } catch (Exception e) {
                        Log.e("MainActivity", "error starting permission intent", e);
                    }
                }
            })
            .show();
    }
```

```java
    new MessageService(this, MessageService.getMyPhoneNumber(this), "THIS IS SMS TEST", null).send();
```



## ToDo
- [x] SMS
- [x] LMS
- [x] APN information
- [x] MMS sender(under Android L)
- [x] MMS sender(above Android L)
- [x] permission cleanup
- [ ] GIF transmitting
- [ ] test

## Tested Phone
- Galaxy S7(Android 7) KT