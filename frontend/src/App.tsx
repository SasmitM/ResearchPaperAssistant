import React, { useState } from 'react';
import './App.css';
import PaperForm from './components/PaperForm';
import PaperAnalysis from './components/PaperAnalysis';
import LoadingSpinner from './components/LoadingSpinner';
import ErrorMessage from './components/ErrorMessage';

interface AnalysisResult {
  arxivId: string;
  title: string;
  authors: string;
  abstractText: string;
  abstractSummary: string;
  fullTextSummary: string;
}

interface PdfExtractionResult {
  success: boolean;
  arxivId: string;
  pdfUrl: string;
  extractionTimeMs: number;
  textLength: number;
  wordCount: number;
  lineCount: number;
  first500Chars: string;
  last500Chars: string;
  fullText: string;
  sectionsFound: {
    hasAbstract: boolean;
    hasIntroduction: boolean;
    hasConclusion: boolean;
    hasReferences: boolean;
  };
}

interface PdfStatsResult {
  success: boolean;
  arxivId: string;
  title?: string; // Added title to PdfStatsResult
  statistics: {
    totalCharacters: number;
    totalWords: number;
    totalLines: number;
    totalParagraphs: number;
    estimatedPages: number;
  };
  textSamples: {
    beginning: string;
    quarter: string;
    middle: string;
    threeQuarters: string;
  };
}

function App() {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<AnalysisResult | null>(null);
  const [pdfExtractionResult, setPdfExtractionResult] = useState<PdfExtractionResult | null>(null);
  const [pdfStatsResult, setPdfStatsResult] = useState<PdfStatsResult | null>(null);
  const [mode, setMode] = useState<'analysis' | 'full-text' | 'stats-only'>('analysis');

  const handleSubmit = async (arxivId: string) => {
    setIsLoading(true);
    setError(null);
    setResult(null);
    setPdfExtractionResult(null);
    setPdfStatsResult(null);
    
    try {
      if (mode === 'full-text') {
        // Get full PDF text extraction
        const response = await fetch(`http://localhost:8080/api/v1/test/pdf/extract/${arxivId}`);
        if (!response.ok) {
          const err = await response.json();
          throw new Error(err.message || 'Failed to extract PDF text');
        }
        const data = await response.json();
        setPdfExtractionResult(data);
      } else if (mode === 'stats-only') {
        // Get PDF extraction statistics only
        const response = await fetch(`http://localhost:8080/api/v1/test/pdf/extract-stats/${arxivId}`);
        if (!response.ok) {
          const err = await response.json();
          throw new Error(err.message || 'Failed to extract PDF statistics');
        }
        const data = await response.json();
        setPdfStatsResult(data);
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
      }
    } catch (err: any) {
      setError(err.message || 'An error occurred.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleNew = () => {
    setResult(null);
    setPdfExtractionResult(null);
    setPdfStatsResult(null);
    setError(null);
  };

  const getLoadingMessage = () => {
    switch (mode) {
      case 'full-text': return 'Extracting full PDF text...';
      case 'stats-only': return 'Extracting PDF statistics...';
      default: return 'Analyzing paper...';
    }
  };

  return (
    <div className="container">
      <h1>Research Paper Assistant</h1>
      
      {/* Mode Toggle */}
      <div style={{ marginBottom: '20px', textAlign: 'center' }}>
        <label style={{ marginRight: '10px' }}>
          <input
            type="radio"
            name="mode"
            value="analysis"
            checked={mode === 'analysis'}
            onChange={(e) => setMode(e.target.value as 'analysis' | 'full-text' | 'stats-only')}
          />
          Full Analysis (AI Summary)
        </label>
        <label style={{ marginRight: '10px' }}>
          <input
            type="radio"
            name="mode"
            value="full-text"
            checked={mode === 'full-text'}
            onChange={(e) => setMode(e.target.value as 'analysis' | 'full-text' | 'stats-only')}
          />
          Full PDF Text
        </label>
        <label>
          <input
            type="radio"
            name="mode"
            value="stats-only"
            checked={mode === 'stats-only'}
            onChange={(e) => setMode(e.target.value as 'analysis' | 'full-text' | 'stats-only')}
          />
          PDF Statistics Only
        </label>
      </div>

      {!result && !pdfExtractionResult && !pdfStatsResult && !isLoading && (
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
      
      {pdfExtractionResult && (
        <>
          <button style={{marginBottom:16}} onClick={handleNew}>Extract Another Paper</button>
          <div className="result">
            <h2>Full PDF Text Extraction</h2>
            <div><strong>arXiv ID:</strong> {pdfExtractionResult.arxivId}</div>
            <div><strong>PDF URL:</strong> <a href={pdfExtractionResult.pdfUrl} target="_blank" rel="noopener noreferrer">{pdfExtractionResult.pdfUrl}</a></div>
            <div><strong>Extraction Time:</strong> {pdfExtractionResult.extractionTimeMs}ms</div>
            <div><strong>Text Length:</strong> {pdfExtractionResult.textLength.toLocaleString()} characters</div>
            <div><strong>Word Count:</strong> {pdfExtractionResult.wordCount.toLocaleString()} words</div>
            <div><strong>Line Count:</strong> {pdfExtractionResult.lineCount.toLocaleString()} lines</div>
            
            <div style={{ margin: '16px 0' }}>
              <strong>Sections Found:</strong>
              <ul style={{ margin: '8px 0', paddingLeft: '20px' }}>
                <li>Abstract: {pdfExtractionResult.sectionsFound.hasAbstract ? '✅' : '❌'}</li>
                <li>Introduction: {pdfExtractionResult.sectionsFound.hasIntroduction ? '✅' : '❌'}</li>
                <li>Conclusion: {pdfExtractionResult.sectionsFound.hasConclusion ? '✅' : '❌'}</li>
                <li>References: {pdfExtractionResult.sectionsFound.hasReferences ? '✅' : '❌'}</li>
              </ul>
            </div>

            <div style={{ margin: '16px 0' }}>
              <strong>Complete Extracted Text:</strong><br />
              <pre style={{
                whiteSpace: 'pre-wrap',
                background: '#fff',
                padding: '12px',
                borderRadius: '4px',
                border: '1px solid #ddd',
                maxHeight: '600px',
                overflow: 'auto',
                fontSize: '14px',
                lineHeight: '1.4'
              }}>
                {pdfExtractionResult.fullText}
              </pre>
            </div>
          </div>
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
    </div>
  );
}

export default App;
