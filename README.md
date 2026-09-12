# ShopForHome

ShopForHome is a home-decor e-commerce application developed as a group capstone for Great Learning's Java Full Stack with GCP course. The assignment explored moving a retail store online through customer shopping features, administration tools, and cloud deployment.

This repository preserves the course submission, including application source, a database model, and deployment documentation.

## My contribution

I am Suraj Salunke. I contributed to **backend development and GCP deployment** as part of the project group.

The group started from an existing code template and adapted it for the capstone. The application combines inherited code and group contributions. The features below describe the overall application; my confirmed contribution areas are backend development and cloud deployment.

## Application features

The repository contains code for:

- Customer registration and authentication.
- Product and category browsing, with product administration.
- Shopping carts, orders, and wishlists.
- Discount coupon management.
- Bulk product imports from CSV.
- Email sending.
- Angular shopping and administration pages, including a sales page.

The course brief additionally specified product sorting, low-stock emails, reports for selected date ranges, and coupons for selected users. Those detailed acceptance criteria still need end-to-end verification.

## Technology stack

| Area | Checked-in configuration |
| --- | --- |
| Backend | Java 11, Spring Boot, Spring MVC, Spring Data JPA, Spring Security, JWT |
| Build | Maven; Spring Boot parent `2.2.0.BUILD-SNAPSHOT` |
| Database | MySQL in the active application properties |
| Frontend | Angular `~7.1.0`, TypeScript `~3.1.6`, RxJS `~6.3.3` |
| UI styling | Bootstrap `^4.0.0` |
| Supporting libraries | Apache Commons CSV and Spring Boot Mail |
| Course deployment | Google Cloud Platform |

The Maven file also declares H2 and PostgreSQL drivers; the current datasource configuration selects MySQL.

## Architecture

The Angular frontend calls REST endpoints in a Spring Boot backend. Controllers delegate to service classes and repositories, with MySQL configured for persistence.

The current source is organized as one backend application. Coupon controllers and service classes are part of that application. The course requirement for a separate reports/coupons microservice is not established by this structure.

The backend uses port `8080`. Development frontend requests target `//localhost:8080`. The production frontend API base is empty, so an appropriate API origin or routing configuration is needed for deployment.

## Repository guide

| Location | Contents |
| --- | --- |
| [backend](backend/) | Spring Boot source, Maven configuration, and backend tests |
| [frontend](frontend/) | Angular source and dependency configuration |
| [Database diagram](DB_schema.jpg) | Schema illustration |
| [Workbench model](ecommerce%20schema.mwb) | MySQL Workbench schema model |
| [Sample product CSV](fileupload.csv) | CSV file supplied with the submission |
| [Cloud screenshots](Cloud%20screenshots/) | Course deployment images |
| [Project report](Project%20Report%20G-9.docx) | Group project report |
| [Deployment document](cloud%20deployment%20snapshots.docx) | Additional deployment screenshots |
| [Final presentation](final%20presentaion%20group-09.pptx) | Group presentation |
| [Original run notes](steps%20to%20run%20webapp%20on%20cloud.txt) | Historical setup instructions |

## Local setup

A fresh build and end-to-end run have not yet been verified during this documentation update. The following is a recovery checklist, pending a tested quick start:

1. Resolve the Java 11/Maven build, including the snapshot parent and Maven plugin configuration.
2. Prepare a local MySQL database using the schema model and entities. Provide database and mail credentials through local configuration.
3. Start `eshop.homedecor.shopapi.ShopApiApplication` and confirm that the API is available on port `8080`.
4. Establish a Node/npm toolchain compatible with the Angular 7 dependencies. The original run notes contain a malformed installation command and require correction.
5. Run the UI on a separate port and confirm its API configuration. The frontend declares `npm start`, `npm run build`, and `npm test` scripts.
6. Exercise the customer and administrator flows before documenting successful behavior.

## Cloud deployment

GCP deployment was part of my contribution to the group project. The linked screenshots and documents preserve the submitted deployment material.

The assignment called for VMs with autoscaling and a startup script fetching application files from Cloud Storage. A reproducible startup script and autoscaling configuration have not been identified in this repository, and a currently running deployment has not been verified.

## Testing and remaining work

The inspected cart and order backend tests contain empty test methods. Maven also sets `testFailureIgnore` to `true`, so a successful Maven result alone would not demonstrate passing tests.

The next improvements are:

- Reproduce the application locally and document tested setup commands.
- Add meaningful backend tests and make test failures fail the build.
- Export a complete SQL schema and provide sample configuration.
- Verify image storage, sales reports, coupon targeting, and low-stock notifications.
- Document or implement the remaining course architecture and deployment requirements.

The assignment requested Material UI; the checked-in frontend declares Bootstrap and does not declare Angular Material.

## Credits

Great Learning provided the capstone assignment. The application was a group submission built from an existing code template, with Suraj Salunke contributing to backend development and GCP deployment.

Original author comments and bundled third-party notices remain in the source. The exact upstream template URL has not yet been confirmed.

[Suraj Salunke on LinkedIn](https://www.linkedin.com/in/suraj-salunke-b57590158/)
