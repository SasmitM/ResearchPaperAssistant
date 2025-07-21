import React, { useState } from 'react';

interface PaperFormProps {
  onSubmit: (arxivId: string) => void;
  isLoading: boolean;
}

const PaperForm: React.FC<PaperFormProps> = ({ onSubmit, isLoading }) => {
  const [arxivId, setArxivId] = useState('');
  const [error, setError] = useState('');

  const validateArxivId = (id: string): boolean => {
    // Accepts 2301.00001 or cs/0301001
    return /^(\d{4}\.\d{4,5}|[a-z\-]+\/\d{7})$/.test(id);
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    if (!arxivId.trim()) {
      setError('Please enter an arXiv ID.');
      return;
    }
    if (!validateArxivId(arxivId.trim())) {
      setError('Invalid arXiv ID format. Example: 2301.00001 or cs/0301001');
      return;
    }
    onSubmit(arxivId.trim());
  };

  return (
    <form onSubmit={handleSubmit}>
      <label htmlFor="arxivId">arXiv ID</label>
      <input
        id="arxivId"
        type="text"
        value={arxivId}
        onChange={e => setArxivId(e.target.value)}
        placeholder="e.g. 2301.00001 or cs/0301001"
        disabled={isLoading}
      />
      {error && <div className="error">{error}</div>}
      <button type="submit" disabled={isLoading}>
        {isLoading ? 'Analyzing...' : 'Analyze Paper'}
      </button>
    </form>
  );
};

export default PaperForm; 