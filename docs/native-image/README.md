## Native image support

This directory contains reflection hints that are required for integrating the add-on into a native image. These hints were verified against add-on version 2.3.3-SNAPSHOT, which depends on Apache POI 5.2.3, Apache FOP 2.7, and docx4j 8.3.8.

Additionally, the following build arguments must be specified:

```
-H:+AddAllCharsets
--initialize-at-run-time=com.sun.xml.bind.v2.runtime.unmarshaller.UnmarshallingContext
--initialize-at-run-time=com.sun.xml.bind.v2.runtime.unmarshaller.UnmarshallerImpl
```

Please note that integrating this add-on into native images is not supported or endorsed by our team. We do not guarantee the functionality, accuracy, or completeness of the information contained in these files. Users are encouraged to use these resources at their own risk and discretion.
