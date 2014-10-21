#include <stdio.h>
#include <avr/io.h>
#include <avr/interrupt.h>
#include <util/delay.h>

#define LED PB5
#ifndef F_CPU
#warning 
#define F_CPU 16000000UL
#endif

#define BAUD 9600UL      // tasa de baudios

// Cálculos

#define UBRR_VAL ((F_CPU+BAUD*8)/(BAUD*16)-1)   // redondeo inteligente
#define BAUD_REAL (F_CPU/(16*(UBRR_VAL+1)))     // tasa real de baudios
#define BAUD_ERROR ((BAUD_REAL*1000)/BAUD) // error en partes por millón, 1000 = no error
#if ((BAUD_ERROR<990) || (BAUD_ERROR>1010))
#error Error in baud rate greater than 1%!
#endif

void uart_init(void) {
	UBRR0H = UBRR_VAL >> 8;
	UBRR0L = UBRR_VAL & 0xFF;

	UCSR0C = (0 << UMSEL01) | (0 << UMSEL00) | (1 << UCSZ01) | (1 << UCSZ00); // asíncronos 8N1
	UCSR0B |= (1 << RXEN0); // habilitar UART RX
	UCSR0B |= (1 << TXEN0); // habilitar  UART TX
	UCSR0B |= (1 << RXCIE0); // deshabilitar
}


uint8_t uart_getc(void) {
	while (!(UCSR0A & (1 << RXC0)))
		// espera a que el símbolo este listo
		;
	return UDR0; // retorna simbolo
}

uint8_t uart_putc(unsigned char data) {
	/* Wait for empty transmit buffer */
	while (!(UCSR0A & (1 << UDRE0)))
		;
	/* asigna el dato en el buffer  lo envia */
	UDR0 = data;
	return 0;
}


void initIO(void) {
	DDRD |= (1 << DDD3);
	DDRB = 0xff;
}


volatile uint8_t data = 10;

int main(void) {
	initIO();
	uart_init();
	sei();

	uint8_t i = 0;
	volatile uint8_t pause;
	for(;;) {
		pause = data;
		PORTB |= (1 << LED);
		for(i = 0; i < pause; i++)
			_delay_us(10);
		PORTB &= ~(1 << LED);
		for(i = 0; i < 255-pause; i++)
			_delay_us(10);
	}
	return 0;
}

ISR(USART_RX_vect) {
	data = UDR0;
}
