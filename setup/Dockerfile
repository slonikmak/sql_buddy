# Step 1: Build the JAR file
FROM clojure:lein AS builder

WORKDIR /usr/src/app

# Copy the entire project to the container
COPY ../ /usr/src/app

# Build the JAR
RUN lein do clean, uberjar

# Step 2: Create a minimal runtime image
FROM openjdk:24-jdk-slim

ENV APP_HOME /usr/src/app

WORKDIR $APP_HOME

# Copy the JAR file from the builder stage
COPY --from=builder /usr/src/app/target/sql-buddy.jar $APP_HOME/sql-buddy.jar

# Expose the application port
EXPOSE 3000

# Set the command to run your application
CMD ["java", "-jar", "sql-buddy.jar"]
