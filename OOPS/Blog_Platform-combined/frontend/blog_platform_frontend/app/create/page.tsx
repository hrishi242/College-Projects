"use client";
import React, { useState, useRef } from 'react';
import { useEditor, EditorContent } from '@tiptap/react';
import StarterKit from '@tiptap/starter-kit';
import { Bold } from '@tiptap/extension-bold';
import { Italic } from '@tiptap/extension-italic';
import { Underline } from '@tiptap/extension-underline';
import { Heading } from '@tiptap/extension-heading';
import { BulletList } from '@tiptap/extension-bullet-list';
import { OrderedList } from '@tiptap/extension-ordered-list';
import { ListItem } from '@tiptap/extension-list-item';
import { Link } from '@tiptap/extension-link';
import { CodeBlock } from '@tiptap/extension-code-block';
import { Table } from '@tiptap/extension-table';
import { TableRow } from '@tiptap/extension-table-row';
import { TableCell } from '@tiptap/extension-table-cell';
import { TableHeader } from '@tiptap/extension-table-header';
import { Image } from '@tiptap/extension-image';
import { Blockquote } from '@tiptap/extension-blockquote';
import { HorizontalRule } from '@tiptap/extension-horizontal-rule';

const CreateBlog = () => {
  const [title, setTitle] = useState('');
  const [isLinkModalOpen, setIsLinkModalOpen] = useState(false);
  const [linkURL, setLinkURL] = useState('');
  const [isImageURLModalOpen, setIsImageURLModalOpen] = useState(false);
  const [imageURL, setImageURL] = useState('');
  const editor = useEditor({
    extensions: [
      StarterKit,
      Bold,
      Italic,
      Underline,
      Heading.configure({
        levels: [1, 2, 3],
      }),
      BulletList,
      OrderedList,
      ListItem,
      Link,
      CodeBlock.configure({
        HTMLAttributes: {
          class: 'rounded-md bg-[#b8b0a7] text-[#151515] p-4 font-mono text-sm',
        },
      }),
      Table,
      TableRow,
      TableHeader,
      TableCell,
      Image,
      Blockquote,
      HorizontalRule,
    ],
    content: '<p>Start writing your blog post here...</p>',
  });

  const imageInputRef = useRef(null);

  const handleTitleChange = (event) => {
    setTitle(event.target.value);
  };

  const handleSave = async () => {
    if (editor) {
      const contentHTML = editor.getHTML();
      const postData = {
        title: title,
        content: contentHTML,
        // You might want to include other metadata like author, date, etc.
      };

      try {
        const response = await fetch('/api/blogs', { // Replace with your actual API endpoint
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify(postData),
        });

        if (response.ok) {
          // Handle success, maybe redirect to the blog post or a success message
          console.log('Blog post saved successfully!');
        } else {
          // Handle error
          console.error('Failed to save blog post:', response.status);
        }
      } catch (error) {
        console.error('Error saving blog post:', error);
      }
    }
  };

  const handleImageUpload = (event) => {
    const file = event.target.files[0];
    if (file) {
      const imageUrl = URL.createObjectURL(file);
      editor.chain().focus().setImage({ src: imageUrl }).run();
    }
  };

  const handleImageViaLink = () => {
    openImageURLModal();
  };

  const triggerImageUpload = () => {
    imageInputRef.current?.click();
  };

  const openLinkModal = () => {
    setIsLinkModalOpen(true);
    setLinkURL(''); // Reset the URL when opening
  };

  const closeLinkModal = () => {
    setIsLinkModalOpen(false);
  };

  const applyLink = () => {
    if (linkURL) {
      let urlToSet = linkURL;
      if (!linkURL.startsWith('http://') && !linkURL.startsWith('https://')) {
        urlToSet = `https://${linkURL}`;
      }
      editor.chain().focus().setLink({ href: urlToSet }).run();
    }
    closeLinkModal();
  };

  const openImageURLModal = () => {
    setIsImageURLModalOpen(true);
    setImageURL('');
  };

  const closeImageURLModal = () => {
    setIsImageURLModalOpen(false);
  };

  const applyImageURL = () => {
    if (imageURL) {
      editor.chain().focus().setImage({ src: imageURL }).run();
    }
    closeImageURLModal();
  };

  if (!editor) {
    return <div>Loading editor...</div>;
  }

  return (
    <div className="min-h-screen bg-[rgb(213,208,202)] text-[rgb(21,21,21)] font-sans relative">
      {/* Subtle Dot Matrix Background */}
      <div className="absolute inset-0 bg-[radial-gradient(circle,rgb(110,110,110,0.2)_1.6px,transparent_1.6px)] bg-[size:14px_14px] opacity-50 z-0"></div>

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

      {/* Create Blog Content */}
      <div className="max-w-4xl mx-auto py-16 px-6 relative z-10">
        <h1 className="text-4xl font-bold text-[rgb(21,21,21)] mb-8">Create New Blog Post</h1>

        <div className="mb-4">
          <label htmlFor="title" className="block text-sm font-medium text-[rgb(21,21,21)]">
            Title:
          </label>
          <input
            type="text"
            id="title"
            className="mt-1 block w-full rounded-md shadow-sm focus:border-indigo-500 focus:ring-indigo-500 text-[rgb(21,21,21)] p-2 bg-white border border-gray-300"
            value={title}
            onChange={handleTitleChange}
          />
        </div>

        {/* Tiptap Toolbar */}
        <div className="mb-2 flex flex-wrap gap-2">
          <button
            onClick={() => editor.chain().focus().toggleBold().run()}
            className={`px-2 py-1 rounded-md text-sm ${editor.isActive('bold') ? 'bg-[rgb(21,21,21)] text-white' : 'bg-gray-200 text-gray-700 hover:bg-gray-300 hover:text-gray-800'} transition font-semibold`}
          >
            Bold
          </button>
          <button
            onClick={() => editor.chain().focus().toggleItalic().run()}
            className={`px-2 py-1 rounded-md text-sm ${editor.isActive('italic') ? 'bg-[rgb(21,21,21)] text-white' : 'bg-gray-200 text-gray-700 hover:bg-gray-300 hover:text-gray-800'} transition font-semibold`}
          >
            Italic
          </button>
          <button
            onClick={() => editor.chain().focus().toggleUnderline().run()}
            className={`px-2 py-1 rounded-md text-sm ${editor.isActive('underline') ? 'bg-[rgb(21,21,21)] text-white' : 'bg-gray-200 text-gray-700 hover:bg-gray-300 hover:text-gray-800'} transition font-semibold`}
          >
            Underline
          </button>

          <button
            onClick={() => editor.chain().focus().toggleOrderedList().run()}
            className={`px-2 py-1 rounded-md text-sm ${editor.isActive('orderedList') ? 'bg-[rgb(21,21,21)] text-white' : 'bg-gray-200 text-gray-700 hover:bg-gray-300 hover:text-gray-800'} transition font-semibold`}
          >
            Ordered List
          </button>
          <button
            onClick={() => editor.chain().focus().toggleBulletList().run()}
            className={`px-2 py-1 rounded-md text-sm ${editor.isActive('bulletList') ? 'bg-[rgb(21,21,21)] text-white' : 'bg-gray-200 text-gray-700 hover:bg-gray-300 hover:text-gray-800'} transition font-semibold`}
          >
            Bullet List
          </button>
          <button
            onClick={() => editor.chain().focus().toggleCodeBlock().run()}
            className={`px-2 py-1 rounded-md text-sm ${editor.isActive('codeBlock') ? 'bg-[rgb(21,21,21)] text-white' : 'bg-gray-200 text-gray-700 hover:bg-gray-300 hover:text-gray-800'} transition font-semibold`}
          >
            Code Block
          </button>
          <div className="relative">
            <select
              onChange={(e) => editor.chain().focus().setHeading({ level: parseInt(e.target.value, 10) }).run()}
              className="block appearance-none w-full bg-gray-200 border border-gray-200 text-gray-700 py-1 px-2 rounded-md leading-tight focus:outline-none focus:bg-white focus:border-gray-500 text-sm pr-8"
              value={editor.isActive('heading', { level: 1 }) ? 1 : editor.isActive('heading', { level: 2 }) ? 2 : editor.isActive('heading', { level: 3 }) ? 3 : ''}
            >
              <option value="">Heading</option>
              <option value="1">H1</option>
              <option value="2">H2</option>
              <option value="3">H3</option>
            </select>
            <div className="pointer-events-none absolute inset-y-0 right-0 flex items-center px-2 text-gray-700">
              <svg className="fill-current h-4 w-4" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20"><path d="M9.293 12.95l.707.707L15.657 8l-1.414-1.414L10 10.828 5.757 6.586 4.343 8z"/></svg>
            </div>
          </div>
          <button
            onClick={openLinkModal}
            className={`px-2 py-1 rounded-md text-sm ${editor.isActive('link') ? 'bg-[rgb(21,21,21)] text-white' : 'bg-gray-200 text-gray-700 hover:bg-gray-300 hover:text-gray-800'} transition font-semibold`}
          >
            Link
          </button>
          {editor.isActive('link') && (
            <button
              onClick={() => editor.chain().focus().unsetLink().run()}
              className="px-2 py-1 rounded-md text-sm bg-red-400 text-white hover:bg-red-500 transition font-semibold"
            >
              Unlink
            </button>
          )}
          <button
            onClick={triggerImageUpload}
            className="px-2 py-1 rounded-md text-sm bg-gray-200 text-gray-700 hover:bg-gray-300 hover:text-gray-800 transition font-semibold"
          >
            Add Image
          </button>
          <button
            onClick={handleImageViaLink}
            className="px-2 py-1 rounded-md text-sm bg-gray-200 text-gray-700 hover:bg-gray-300 hover:text-gray-800 transition font-semibold"
          >
            Image from URL
          </button>
          <button
            onClick={() => editor.chain().focus().toggleBlockquote().run()}
            className={`px-2 py-1 rounded-md text-sm ${editor.isActive('blockquote') ? 'bg-[rgb(21,21,21)] text-white' : 'bg-gray-200 text-gray-700 hover:bg-gray-300 hover:text-gray-800'} transition font-semibold`}
          >
            Blockquote
          </button>
          <button
            onClick={() => editor.chain().focus().insertTable({ rows: 3, cols: 3 }).run()}
            className="px-2 py-1 rounded-md text-sm bg-gray-200 text-gray-700 hover:bg-gray-300 hover:text-gray-800 transition font-semibold"
          >
            Table
          </button>
          <button
            onClick={() => editor.chain().focus().setHorizontalRule().run()}
            className="px-2 py-1 rounded-md text-sm bg-gray-200 text-gray-700 hover:bg-gray-300 hover:text-gray-800 transition font-semibold"
          >
            Horizontal Rule
          </button>
        </div>

        <input
          type="file"
          accept="image/*"
          ref={imageInputRef}
          className="hidden"
          onChange={handleImageUpload}
        />

        <div className="rounded-md overflow-hidden bg-[#fefefe] p-4 editor-wrapper border border-gray-300">
          <EditorContent editor={editor} className="prose prose-sm max-w-none [&_ol]:list-decimal [&_ul]:list-disc" />
        </div>

        {/* Link Modal */}
        {isLinkModalOpen && (
          <div className="fixed inset-0 overflow-y-auto h-full w-full flex items-center justify-center z-20 backdrop-blur-md bg-black/30 transition-opacity duration-1100 ease-in-out">
            <div className="relative p-8 bg-[rgb(213,208,202)] text-[rgb(21,21,21)] w-full max-w-md rounded-md transition-transform duration-1100 ease-in-out">
              <h2 className="text-lg font-bold mb-4">Insert Link</h2>
              <label htmlFor="link-url" className="block text-sm font-medium">
                URL:
              </label>
              <input
                type="text"
                id="link-url"
                className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 p-2"
                value={linkURL}
                onChange={(e) => setLinkURL(e.target.value)}
              />
              <div className="mt-4 flex justify-end gap-2">
                <button
                  onClick={applyLink}
                  className="px-4 py-2 bg-[rgb(21,21,21)] text-white rounded-lg hover:bg-[rgb(51,51,51)] focus:outline-none focus:ring-2 focus:ring-indigo-500 font-semibold transition"
                >
                  Apply
                </button>
                <button
                  onClick={closeLinkModal}
                  className="px-4 py-2 bg-gray-300 text-gray-700 rounded-lg hover:bg-gray-400 focus:outline-none focus:ring-2 focus:ring-gray-500 font-semibold transition"
                >
                  Cancel
                </button>
              </div>
            </div>
          </div>
        )}

        {/* Image URL Modal */}
        {isImageURLModalOpen && (
          <div className="fixed inset-0 overflow-y-auto h-full w-full flex items-center justify-center z-20 backdrop-blur-md bg-black/30 transition-opacity duration-1100 ease-in-out">
            <div className="relative p-8 bg-[rgb(213,208,202)] text-[rgb(21,21,21)] w-full max-w-md rounded-md transition-transform duration-1100 ease-in-out">
              <h2 className="text-lg font-bold mb-4">Insert Image from URL</h2>
              <label htmlFor="image-url" className="block text-sm font-medium">
                Image URL:
              </label>
              <input
                type="text"
                id="image-url"
                className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 p-2"
                value={imageURL}
                onChange={(e) => setImageURL(e.target.value)}
              />
              <div className="mt-4 flex justify-end gap-2">
                <button
                  onClick={applyImageURL}
                  className="px-4 py-2 bg-[rgb(21,21,21)] text-white rounded-lg hover:bg-[rgb(51,51,51)] focus:outline-none focus:ring-2 focus:ring-indigo-500 font-semibold transition"
                >
                  Apply
                </button>
                <button
                  onClick={closeImageURLModal}
                  className="px-4 py-2 bg-gray-300 text-gray-700 rounded-lg hover:bg-gray-400 focus:outline-none focus:ring-2 focus:ring-gray-500 font-semibold transition"
                >
                  Cancel
                </button>
              </div>
            </div>
          </div>
        )}

        <button
          onClick={handleSave}
          className="mt-4 px-8 py-4 bg-[rgb(21,21,21)] text-white rounded-lg hover:bg-[rgb(51,51,51)] transition duration-1100 ease-in-out font-semibold"
        >
          Save Blog Post
        </button>
      </div>
    </div>
  );
};

export default CreateBlog;