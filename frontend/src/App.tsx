import React, { useState } from 'react';
import './App.css';
import PaperForm from './components/PaperForm';
import PaperAnalysis from './components/PaperAnalysis';
import LoadingSpinner from './components/LoadingSpinner';
import ErrorMessage from './components/ErrorMessage';
import Chatbot from './components/Chatbot';

interface AnalysisResult {
  arxivId: string;
  title: string;
  authors: string;
  abstractText: string;
  abstractSummary: string;
  fullTextSummary: string;
}

interface PdfStatsResult {
  success: boolean;
  arxivId: string;
  title?: string;
  statistics: {
    totalCharacters: number;
    totalWords: number;
    totalLines: number;
    totalParagraphs: number;
    estimatedPages: number;
  };
}


function App() {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<AnalysisResult | null>(null);
  const [pdfStatsResult, setPdfStatsResult] = useState<PdfStatsResult | null>(null);
  const [currentArxivId, setCurrentArxivId] = useState<string | null>(null);
  const [currentPaperTitle, setCurrentPaperTitle] = useState<string | null>(null);
  const [mode, setMode] = useState<'analysis' | 'stats-only' | 'chatbot'>('analysis');

  const handleSubmit = async (arxivId: string) => {
    setIsLoading(true);
    setError(null);
    setResult(null);
    setPdfStatsResult(null);
    setCurrentArxivId(arxivId);
    
    try {
      if (mode === 'stats-only') {
        // Get PDF extraction statistics only
        const response = await fetch(`http://localhost:8080/api/v1/test/pdf/extract-stats/${arxivId}`);
        if (!response.ok) {
          const err = await response.json();
          throw new Error(err.message || 'Failed to extract PDF statistics');
        }
        const data = await response.json();
        setPdfStatsResult(data);
        setCurrentPaperTitle(data.title || null);
      } else if (mode === 'chatbot') {
        // For chatbot mode, we need to ensure the paper exists first
        const existsResponse = await fetch(`http://localhost:8080/api/v1/papers/test/${arxivId}/exists`);
        if (!existsResponse.ok) {
          throw new Error('Failed to check if paper exists');
        }
        const existsData = await existsResponse.json();
        
        if (!existsData.paperCached) {
          // Try to fetch paper metadata to get the title
          const analyzeRes = await fetch('http://localhost:8080/api/v1/papers/analyze', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ arxivId })
          });
          if (!analyzeRes.ok) {
            throw new Error('Paper not found. Please analyze it first.');
          }
          // Wait a bit for the analysis to complete
          await new Promise(resolve => setTimeout(resolve, 2000));
        }
        
        // Get paper details for title
        const paperResponse = await fetch(`http://localhost:8080/api/v1/papers/${arxivId}`);
        if (paperResponse.ok) {
          const paperData = await paperResponse.json();
          setCurrentPaperTitle(paperData.title || null);
        }
        
        // Chatbot mode doesn't need to set any results, just the arxivId and title
        setIsLoading(false);
        return;
      } else {
        // Full analysis flow
        const analyzeRes = await fetch('http://localhost:8080/api/v1/papers/analyze', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ arxivId })
        });
        if (!analyzeRes.ok) {
          const err = await analyzeRes.json();
          throw new Error(err.message || 'Failed to submit paper for analysis');
        }
        const { jobId } = await analyzeRes.json();
        
        // Poll for job completion
        let status = 'PENDING';
        let attempts = 0;
        while (status !== 'COMPLETED' && attempts < 30) {
          await new Promise(res => setTimeout(res, 1000));
          const statusRes = await fetch(`http://localhost:8080/api/v1/papers/jobs/${jobId}`);
          const statusData = await statusRes.json();
          status = statusData.status;
          if (status === 'FAILED') throw new Error('Analysis failed.');
          attempts++;
        }
        if (status !== 'COMPLETED') throw new Error('Analysis timed out.');
        
        // Get analysis result
        const resultRes = await fetch(`http://localhost:8080/api/v1/papers/${arxivId}`);
        if (!resultRes.ok) throw new Error('Failed to fetch analysis result.');
        const data = await resultRes.json();
        setResult({
          arxivId: data.arxivId,
          title: data.title,
          authors: data.authors,
          abstractText: data.abstractText,
          abstractSummary: data.abstractSummary,
          fullTextSummary: data.fullTextSummary,
        });
        setCurrentPaperTitle(data.title || null);
      }
    } catch (err: any) {
      setError(err.message || 'An error occurred.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleNew = () => {
    setResult(null);
    setPdfStatsResult(null);
    setError(null);
    setCurrentArxivId(null);
    setCurrentPaperTitle(null);
  };

  const getLoadingMessage = () => {
    switch (mode) {
      case 'stats-only': return 'Extracting PDF statistics...';
      case 'chatbot': return 'Preparing chatbot...';
      default: return 'Analyzing paper...';
    }
  };

  return (
    <div className="container">
      <h1 style={{ textAlign: 'center' }}>Research Paper Assistant</h1>
      
      {/* Mode Toggle */}
      <div style={{ marginBottom: '20px', textAlign: 'center' }}>
        <label style={{ marginRight: '10px' }}>
          <input
            type="radio"
            name="mode"
            value="analysis"
            checked={mode === 'analysis'}
            onChange={(e) => setMode(e.target.value as 'analysis' | 'stats-only' | 'chatbot')}
          />
          Full Analysis (AI Summary)
        </label>
        <label style={{ marginRight: '10px' }}>
          <input
            type="radio"
            name="mode"
            value="stats-only"
            checked={mode === 'stats-only'}
            onChange={(e) => setMode(e.target.value as 'analysis' | 'stats-only' | 'chatbot')}
          />
          PDF Statistics Only
        </label>
        <label>
          <input
            type="radio"
            name="mode"
            value="chatbot"
            checked={mode === 'chatbot'}
            onChange={(e) => setMode(e.target.value as 'analysis' | 'stats-only' | 'chatbot')}
          />
          Chatbot
        </label>
      </div>

      {!result && !pdfStatsResult && !currentArxivId && !isLoading && (
        <PaperForm onSubmit={handleSubmit} isLoading={isLoading} />
      )}
      
      {isLoading && <LoadingSpinner message={getLoadingMessage()} />}
      
      {error && <ErrorMessage message={error} />}
      
      {result && (
        <>
          <button style={{marginBottom:16}} onClick={handleNew}>Analyze Another Paper</button>
          <PaperAnalysis analysis={result} />
        </>
      )}

      {pdfStatsResult && (
        <>
          <button style={{marginBottom:16}} onClick={handleNew}>Extract Another Paper</button>
          <div className="result">
            <h2>PDF Extraction Statistics</h2>
            <div><strong>arXiv ID:</strong> {pdfStatsResult.arxivId}</div>
            {pdfStatsResult.title && (
              <div><strong>Title:</strong> {pdfStatsResult.title}</div>
            )}
            <div style={{ margin: '16px 0' }}>
              <strong>Statistics:</strong>
              <ul style={{ margin: '8px 0', paddingLeft: '20px' }}>
                <li>Total Characters: {pdfStatsResult.statistics.totalCharacters.toLocaleString()}</li>
                <li>Total Words: {pdfStatsResult.statistics.totalWords.toLocaleString()}</li>
                <li>Total Lines: {pdfStatsResult.statistics.totalLines.toLocaleString()}</li>
                <li>Total Paragraphs: {pdfStatsResult.statistics.totalParagraphs.toLocaleString()}</li>
                <li>Estimated Pages: {pdfStatsResult.statistics.estimatedPages}</li>
              </ul>
            </div>
          </div>
        </>
      )}

      {mode === 'chatbot' && currentArxivId && !isLoading && (
        <>
          <button style={{marginBottom:16}} onClick={handleNew}>Chat with Another Paper</button>
          <Chatbot arxivId={currentArxivId} paperTitle={currentPaperTitle || undefined} />
        </>
      )}
    </div>
  );
}

export default App;
