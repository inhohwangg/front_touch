import 'package:flutter/material.dart';
import 'package:android_intent_plus/android_intent.dart';
import 'package:android_intent_plus/flag.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      home: Scaffold(
        appBar: AppBar(title: const Text("카메라 터치 스크린샷")),
        body: Center(
          child: ElevatedButton(
            onPressed: () {
              const intent = AndroidIntent(
                action: 'android.settings.ACCESSIBILITY_SETTINGS',
                flags: <int>[Flag.FLAG_ACTIVITY_NEW_TASK],
              );
              intent.launch();
            },
            child: const Text("접근성 서비스 활성화"),
          ),
        ),
      ),
    );
  }
}
