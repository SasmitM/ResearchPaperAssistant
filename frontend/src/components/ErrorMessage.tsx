import React from 'react';

const ErrorMessage: React.FC<{ message: string }> = ({ message }) => (
  <div className="error">
    <strong>Error:</strong> {message}
  </div>
);

export default ErrorMessage; 