// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "ChatPlusPlus",
    platforms: [
        .iOS(.v15)
    ],
    products: [
        .library(
            name: "ChatPlusPlus",
            targets: ["ChatPlusPlus"]),
    ],
    dependencies: [
        .package(url: "https://github.com/firebase/firebase-ios-sdk.git", from: "10.15.0"),
    ],
    targets: [
        .target(
            name: "ChatPlusPlus",
            dependencies: [
                .product(name: "FirebaseFirestore", package: "firebase-ios-sdk"),
                .product(name: "FirebaseAuth", package: "firebase-ios-sdk"),
                .product(name: "FirebaseStorage", package: "firebase-ios-sdk"),
            ]),
    ]
)
