# MessagingKR

MessageKR is SMS and MMS sender for korean Telecom providers.

This project inspired by [AndroidMMS](https://github.com/kakopappa/AndroidMMS)

MessageKR은 한국 통신사를 위한 SMS/MMS 래퍼입니다.

## Usage

```groovy
dependencies {
    ...
    compile 'me.kudryavka:MessageKR:0.0.1-ALPHA'
    ...
}
```

```java
    MessageService messageService = new MessageService(this);
    messageService.sendMessage(MessageService.getMyPhoneNumber(this), "THIS IS SMS TEST", null);
```

## ToDo
- [x] SMS
- [x] LMS
- [x] APN informarion
- [ ] MMS sender(under Android L)
- [ ] MMS sender(above Android L)
- [ ] permission cleanup
