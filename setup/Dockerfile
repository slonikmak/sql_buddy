# Step 1: Use an official leindocker run -p 3000:3000 test-cljs-app runtime as a parent image
FROM clojure:lein

# Step 2: Set environment variables
ENV APP_HOME /usr/src/app

# Step 4: Create the application directory
RUN mkdir -p $APP_HOME

# Step 5: Set the working directory inside the container
WORKDIR $APP_HOME

# Step 6: Copy the project files into the container
COPY ../target/sql-buddy.jar $APP_HOME

# Step 8: Expose the application port
EXPOSE 3000

# Step 9: Set the command to run your application
CMD ["java", "-jar", "sql-buddy.jar"]
