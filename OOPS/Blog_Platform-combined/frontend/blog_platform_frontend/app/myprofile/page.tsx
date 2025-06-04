"use client";

import React, { useState, useEffect } from "react";
import Link from "next/link";

const MyProfilePage = () => {
  const [userData, setUserData] = useState({
    name: "",
    email: "",
    bio: "",
    socialMedia: {
      twitter: "",
      linkedin: "",
      github: "",
    },
    blogPosts: [],
  });

  // Simulate fetching user data from an API
  useEffect(() => {
    // Replace this with an actual API call to fetch user data
    const fetchUserData = async () => {
      // Simulated API response
      const response = {
        name: "John Doe",
        email: "john.doe@example.com",
        bio: "Software Developer with a passion for creating innovative solutions.",
        socialMedia: {
          twitter: "https://twitter.com/johndoe",
          linkedin: "https://linkedin.com/in/johndoe",
          github: "https://github.com/johndoe",
        },
        blogPosts: [
          { id: 1, title: "My First Blog Post", content: "This is the content of the first blog post." },
          { id: 2, title: "Another Blog Post", content: "This is the content of the second blog post." },
        ],
      };
      setUserData(response);
    };

    fetchUserData();
  }, []);

  const handleChange = (e) => {
    setUserData({ ...userData, [e.target.name]: e.target.value });
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    console.log("Profile updated:", userData);
    // Later, you will connect this to your backend to update user data
  };

  return (
    <div className="min-h-screen bg-[rgb(213,208,202)] text-[rgb(21,21,21)] relative font-sans">
      {/* Denser Dot Matrix Background */}
      <div className="absolute inset-0 bg-[radial-gradient(circle,rgb(110,110,110,0.2)_1px,transparent_1px)] bg-[size:8px_8px] opacity-50"></div>

      {/* Navbar */}
      <nav className="flex justify-between items-center p-6 bg-[rgb(213,208,202)] relative z-10">
        <div className="text-2xl font-extrabold tracking-wide text-[rgb(21,21,21)]">blog.proj</div>
        <div className="space-x-6 text-sm font-medium text-[rgb(21,21,21)]">
          <a href="/" className="hover:text-[rgb(151,151,151)] transition">Home</a>
          <a href="/about" className="hover:text-[rgb(151,151,151)] transition">About</a>
          <a href="/create" className="hover:text-[rgb(151,151,151)] transition">Create Blog</a>
          <a href="/signup" className="hover:text-[rgb(151,151,151)] transition">Create Account</a>
          <a href="/signin" className="hover:text-[rgb(151,151,151)] transition">Sign In</a>
          <a href="/myprofile" className="hover:text-[rgb(151,151,151)] transition">My Profile</a>
        </div>
      </nav>

      {/* My Profile Form */}
      <div className="p-8 w-full max-w-lg mx-auto mt-20 relative z-10">
        <h1 className="text-3xl font-bold text-center mb-6">My Profile</h1>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-[rgb(21,21,21)]">Name</label>
            <input
              type="text"
              name="name"
              value={userData.name}
              onChange={handleChange}
              className="w-full p-2 border rounded-md bg-[rgb(213,208,202)] focus:outline-none focus:ring-2 focus:ring-[rgb(151,151,151)]"
              required
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-[rgb(21,21,21)]">Email</label>
            <input
              type="email"
              name="email"
              value={userData.email}
              onChange={handleChange}
              className="w-full p-2 border rounded-md bg-[rgb(213,208,202)] focus:outline-none focus:ring-2 focus:ring-[rgb(151,151,151)]"
              required
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-[rgb(21,21,21)]">Bio</label>
            <textarea
              name="bio"
              value={userData.bio}
              onChange={handleChange}
              className="w-full p-2 border rounded-md bg-[rgb(213,208,202)] focus:outline-none focus:ring-2 focus:ring-[rgb(151,151,151)]"
              rows="4"
              required
            />
          </div>
          <button
            type="submit"
            className="w-full py-2 bg-[rgb(21,21,21)] text-white rounded-lg hover:bg-[rgb(51,51,51)] transition font-semibold">
            Update Profile
          </button>
        </form>

        {/* Social Media Links */}
        <div className="mt-8">
          <h2 className="text-2xl font-semibold mb-4">Social Media</h2>
          <div className="space-y-2">
            {userData.socialMedia.twitter && (
              <a href={userData.socialMedia.twitter} target="_blank" rel="noopener noreferrer" className="block text-[rgb(21,21,21)] hover:text-[rgb(151,151,151)] transition">
                Twitter
              </a>
            )}
            {userData.socialMedia.linkedin && (
              <a href={userData.socialMedia.linkedin} target="_blank" rel="noopener noreferrer" className="block text-[rgb(21,21,21)] hover:text-[rgb(151,151,151)] transition">
                LinkedIn
              </a>
            )}
            {userData.socialMedia.github && (
              <a href={userData.socialMedia.github} target="_blank" rel="noopener noreferrer" className="block text-[rgb(21,21,21)] hover:text-[rgb(151,151,151)] transition">
                GitHub
              </a>
            )}
          </div>
        </div>

        {/* Blog Posts */}
        <div className="mt-8">
          <h2 className="text-2xl font-semibold mb-4">My Blog Posts</h2>
          <div className="space-y-4">
            {userData.blogPosts.map((post) => (
              <div key={post.id} className="p-4 border rounded-md bg-[rgb(213,208,202)]">
                <h3 className="text-xl font-semibold">{post.title}</h3>
                <p className="mt-2">{post.content}</p>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};

export default MyProfilePage;
