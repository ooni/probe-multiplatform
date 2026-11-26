// swift-tools-version: 5.9
import PackageDescription
let package = Package(
  name: "KotlinMultiplatformLinkedPackage",
  platforms: [
    .iOS("15.0")
  ],
  products: [
    .library(
      name: "KotlinMultiplatformLinkedPackage",
      type: .none,
      targets: ["KotlinMultiplatformLinkedPackage"]
    )
  ],
  dependencies: [
    .package(
      path: "../Packages/Oonimkall"
    ),
    .package(
      path: "../Packages/Passport"
    ),
    .package(
      url: "https://github.com/ArtSabintsev/Siren",
      from: "6.1.3"
    ),
    .package(
      url: "https://github.com/swiftlang/swift-toolchain-sqlite",
      from: "1.0.13"
    ),
    .package(
      url: "https://github.com/getsentry/sentry-cocoa.git",
      exact: "9.35.0"
    )
  ],
  targets: [
    .target(
      name: "KotlinMultiplatformLinkedPackage",
      dependencies: [
        .product(
          name: "oonimkall",
          package: "Oonimkall"
        ),
        .product(
          name: "Passport",
          package: "Passport"
        ),
        .product(
          name: "Siren",
          package: "Siren"
        ),
        .product(
          name: "SwiftToolchainCSQLite",
          package: "swift-toolchain-sqlite"
        ),
        .product(
          name: "Sentry",
          package: "sentry-cocoa",
          condition: .when(platforms: [.iOS])
        )
      ]
    )
  ]
)
