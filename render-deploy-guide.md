# Deploy to Render Guide

## Prerequisites
- GitHub repository with your code
- Render account (free tier available)

## Step 1: Prepare Your Repository
1. Push your code including the Dockerfile to GitHub
2. Ensure your `application.yml` uses environment variables

## Step 2: Create Web Service on Render

### Option A: Docker Build (Recommended)
1. Go to [Render Dashboard](https://dashboard.render.com)
2. Click "New +" → "Web Service"
3. Connect your GitHub repository
4. Configure:
   - **Name**: readme-generator
   - **Environment**: Docker
   - **Region**: Choose closest to your users
   - **Branch**: main
   - **Build Command**: (leave empty - Docker handles this)
   - **Start Command**: (leave empty - Docker handles this)

### Option B: Native Build (If Dockerfile not pushed)
1. Go to [Render Dashboard](https://dashboard.render.com)
2. Click "New +" → "Web Service"
3. Connect your GitHub repository
4. Configure:
   - **Name**: readme-generator
   - **Environment**: Java
   - **Region**: Choose closest to your users
   - **Branch**: main
   - **Build Command**: `./mvnw clean package -DskipTests`
   - **Start Command**: `java -jar target/Readme-0.0.1-SNAPSHOT.jar`

## Step 3: Set Environment Variables
Add these in Render's Environment Variables section:

```
MONGODB_URI=your_mongodb_connection_string
GITHUB_CLIENT_ID=your_github_client_id
GITHUB_CLIENT_SECRET=your_github_client_secret
JWT_SECRET=your_jwt_secret_key
GEMINI_API_KEY=your_gemini_api_key
```

## Step 4: Update GitHub OAuth Settings
1. Go to GitHub → Settings → Developer settings → OAuth Apps
2. Update Authorization callback URL to: `https://your-app-name.onrender.com/api/auth/callback`
3. Update Homepage URL to: `https://your-app-name.onrender.com`

## Step 5: Update Application Configuration
Update redirect URIs in your application.yml to use your Render URL instead of localhost.

## Step 6: Deploy
1. Click "Create Web Service"
2. Render will automatically build and deploy your application
3. Monitor the build logs for any issues

## Important Notes
- Free tier sleeps after 15 minutes of inactivity
- First request after sleep takes ~30 seconds to wake up
- Use environment variables for all sensitive data
- MongoDB Atlas free tier works well with Render free tier