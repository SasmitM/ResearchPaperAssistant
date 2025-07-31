import React, { useState } from 'react';

interface ChatMessage {
  id: string;
  question: string;
  answer: string;
  timestamp: Date;
  isUser: boolean;
}

interface ChatbotProps {
  arxivId: string;
  paperTitle?: string;
}

const Chatbot: React.FC<ChatbotProps> = ({ arxivId, paperTitle }) => {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [inputValue, setInputValue] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [isButtonDisabled, setIsButtonDisabled] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!inputValue.trim() || isLoading || isButtonDisabled) return;

    const question = inputValue.trim();
    setInputValue('');
    setIsLoading(true);
    setIsButtonDisabled(true); // Disable button immediately

    // Add user message
    const userMessage: ChatMessage = {
      id: Date.now().toString(),
      question,
      answer: '',
      timestamp: new Date(),
      isUser: true
    };
    setMessages(prev => [...prev, userMessage]);

    try {
      const response = await fetch(`http://localhost:8080/api/v1/papers/${arxivId}/ask`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ question })
      });

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.message || 'Failed to get answer');
      }

      const data = await response.json();
      
      // Add AI response
      const aiMessage: ChatMessage = {
        id: data.questionId,
        question: '',
        answer: data.answer,
        timestamp: new Date(data.timestamp),
        isUser: false
      };
      setMessages(prev => [...prev, aiMessage]);

    } catch (error: any) {
      // Add error message
      const errorMessage: ChatMessage = {
        id: Date.now().toString(),
        question: '',
        answer: `Error: ${error.message}`,
        timestamp: new Date(),
        isUser: false
      };
      setMessages(prev => [...prev, errorMessage]);
    } finally {
      setIsLoading(false);
      
      // Add 1-second buffer after response completes
      setTimeout(() => {
        setIsButtonDisabled(false);
      }, 1000);
    }
  };

  const getButtonText = () => {
    if (isLoading) return '🤔 Thinking...';
    if (isButtonDisabled && !isLoading) return '⏳ Please wait...';
    return 'Send';
  };

  const getButtonTooltip = () => {
    if (isLoading) return 'Waiting for the chatbot to respond...';
    if (isButtonDisabled && !isLoading) return 'Please wait 1 second before asking another question...';
    return 'Ask a question about this paper';
  };

  return (
    <div style={{ 
      display: 'flex', 
      flexDirection: 'column', 
      height: '600px', 
      border: '1px solid #ddd', 
      borderRadius: '8px',
      backgroundColor: '#f9f9f9'
    }}>
      {/* Header */}
      <div style={{ 
        padding: '16px', 
        backgroundColor: '#2563eb', 
        color: 'white', 
        borderTopLeftRadius: '8px',
        borderTopRightRadius: '8px'
      }}>
        <h3 style={{ margin: 0 }}>💬 Paper Chatbot</h3>
        <div style={{ fontSize: '14px', opacity: 0.9 }}>
          Ask questions about: {paperTitle || arxivId}
        </div>
      </div>

      {/* Messages */}
      <div style={{ 
        flex: 1, 
        overflowY: 'auto', 
        padding: '16px',
        display: 'flex',
        flexDirection: 'column',
        gap: '12px'
      }}>
        {messages.length === 0 && (
          <div style={{ 
            textAlign: 'center', 
            color: '#666', 
            fontStyle: 'italic',
            marginTop: '20px'
          }}>
            Ask a question about this paper to get started!
          </div>
        )}
        
        {messages.map((message) => (
          <div key={message.id} style={{
            display: 'flex',
            justifyContent: message.isUser ? 'flex-end' : 'flex-start'
          }}>
            <div style={{
              maxWidth: '70%',
              padding: '12px 16px',
              borderRadius: '12px',
              backgroundColor: message.isUser ? '#2563eb' : 'white',
              color: message.isUser ? 'white' : '#333',
              boxShadow: '0 1px 3px rgba(0,0,0,0.1)',
              wordWrap: 'break-word'
            }}>
              {message.isUser ? (
                <div>
                  <div style={{ fontWeight: 'bold', marginBottom: '4px' }}>You:</div>
                  <div>{message.question}</div>
                </div>
              ) : (
                <div>
                  <div style={{ fontWeight: 'bold', marginBottom: '4px' }}>AI:</div>
                  <div style={{ whiteSpace: 'pre-wrap' }}>{message.answer}</div>
                </div>
              )}
              <div style={{ 
                fontSize: '12px', 
                opacity: 0.7, 
                marginTop: '8px',
                textAlign: 'right'
              }}>
                {message.timestamp.toLocaleTimeString()}
              </div>
            </div>
          </div>
        ))}
        
        {isLoading && (
          <div style={{
            display: 'flex',
            justifyContent: 'flex-start'
          }}>
            <div style={{
              padding: '12px 16px',
              borderRadius: '12px',
              backgroundColor: 'white',
              boxShadow: '0 1px 3px rgba(0,0,0,0.1)'
            }}>
              <div style={{ fontWeight: 'bold', marginBottom: '4px' }}>AI:</div>
              <div>🤔 Thinking...</div>
            </div>
          </div>
        )}
      </div>

      {/* Input */}
      <form onSubmit={handleSubmit} style={{ 
        padding: '16px', 
        borderTop: '1px solid #ddd',
        backgroundColor: 'white',
        borderBottomLeftRadius: '8px',
        borderBottomRightRadius: '8px'
      }}>
        <div style={{ display: 'flex', gap: '8px' }}>
          <input
            type="text"
            value={inputValue}
            onChange={(e) => setInputValue(e.target.value)}
            placeholder="Ask a question about this paper..."
            disabled={isLoading || isButtonDisabled}
            style={{
              flex: 1,
              padding: '12px',
              border: '1px solid #ddd',
              borderRadius: '6px',
              fontSize: '14px',
              opacity: (isLoading || isButtonDisabled) ? 0.6 : 1
            }}
          />
          <button
            type="submit"
            disabled={isLoading || isButtonDisabled || !inputValue.trim()}
            title={getButtonTooltip()}
            style={{
              padding: '12px 20px',
              backgroundColor: (isLoading || isButtonDisabled) ? '#b0b8c1' : '#2563eb',
              color: 'white',
              border: 'none',
              borderRadius: '6px',
              cursor: (isLoading || isButtonDisabled) ? 'not-allowed' : 'pointer',
              fontSize: '14px',
              opacity: (isLoading || isButtonDisabled || !inputValue.trim()) ? 0.5 : 1,
              transition: 'all 0.2s ease'
            }}
          >
            {getButtonText()}
          </button>
        </div>
        
        {/* Status indicator */}
        {(isLoading || isButtonDisabled) && (
          <div style={{
            fontSize: '12px',
            color: '#666',
            marginTop: '8px',
            textAlign: 'center',
            fontStyle: 'italic'
          }}>
            {isLoading ? 'Waiting for the chatbot to respond...' : 'Please wait 1 second before asking another question...'}
          </div>
        )}
      </form>
    </div>
  );
};

export default Chatbot; 