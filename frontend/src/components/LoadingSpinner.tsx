import React from 'react';

const LoadingSpinner: React.FC<{ message?: string }> = ({ message = 'Loading...' }) => (
  <div className="loading">
    <div style={{ fontSize: 32, marginBottom: 8 }}>⏳</div>
    {message}
  </div>
);

export default LoadingSpinner; 