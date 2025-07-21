import React from 'react';

interface PaperAnalysisProps {
  analysis: {
    arxivId: string;
    title: string;
    authors: string;
    abstractText: string;
    abstractSummary: string;
    fullTextSummary: string;
  };
}

const PaperAnalysis: React.FC<PaperAnalysisProps> = ({ analysis }) => (
  <div className="result">
    <h2>Paper: {analysis.title}</h2>
    <div><strong>arXiv ID:</strong> {analysis.arxivId}</div>
    <div><strong>Authors:</strong> {analysis.authors}</div>
    <div style={{ margin: '16px 0' }}><strong>Abstract:</strong><br />{analysis.abstractText}</div>
    <div style={{ margin: '16px 0' }}><strong>AI Abstract Summary:</strong><br />{analysis.abstractSummary}</div>
            <div style={{ margin: '16px 0' }}><strong>AI Full Text Summary:</strong><br /><pre style={{whiteSpace:'pre-wrap',background:'#fff',padding:'8px',borderRadius:'4px'}}>{analysis.fullTextSummary}</pre></div>
  </div>
);

export default PaperAnalysis; 